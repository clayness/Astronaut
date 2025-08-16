package wikalloy

import edu.mit.csail.sdg.alloy4.XMLNode
import edu.mit.csail.sdg.translator.A4SolutionReader
import org.apache.logging.log4j.scala.Logging
import org.apache.spark.{SparkConf, SparkContext}
import wikalloy.concrete.{ConcreteImpl, ConcreteLoadFactory}
import wikalloy.generic.AbstractLoad
import wikalloy.objmodel.ObjectModel

import java.nio.file.{Files, Path}
import java.sql.DriverManager
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.util.Using
import scala.util.control.NonFatal

class SparkAnalyzer extends Serializable with Logging {

  def analyze(solutions: List[String], abstractLoads: List[AbstractLoad], oodm: ObjectModel): List[MeasurementResult] = {
    val conf = new SparkConf().setAppName("Wikalloy")
      .set("spark.akka.frameSize", "200")
      .set("spark.default.parallelism", "16")
      .set("spark.storage.blockManagerSlaveTimeoutMs", "600000")
      .set("spark.worker.timeout", "600000")
      .set("spark.akka.timeout", "600000")
      .set("spark.rpc.askTimeout", "600000")
      .set("spark.rpc.retry.wait", "600000")
      .set("spark.rpc.message.maxSize", "300")
      .set("spark.storage.memoryFraction", "0.9")

    val sc = new SparkContext(conf)
    // create a resilient distributed database (RDD) from the
    // list of implementation / function pairs
    val rdd = sc.parallelize(solutions)
    // map the RDD across the Spark cluster, running the analysis
    // function on each pair
    val collectedResult = rdd.map(p => measure(p, abstractLoads, oodm)).collect()
    // stop the Spark cluster
    sc.stop()
    // return the result
    collectedResult.toList
  }

  private def measure(p: String, abstractLoads: List[AbstractLoad], oodm: ObjectModel) = {
    // get the models from the mapping specification and generate
    // concrete loads for each one
    val sol1 = Using(Files.newBufferedReader(Path.of(p))) { pr => A4SolutionReader.read(null, new XMLNode(pr)) }.get
    // generate the concrete loads
    val e = new ConcreteLoadFactory(sol1, oodm).create(abstractLoads.asJava);
    computeMetricsForImpl(Path.of(p), e)
  }

  private def computeMetricsForImpl(solution: Path, impl: ConcreteImpl): MeasurementResult = {
    val jdbcUrl = WikalloyConfig.getMySQLUri
    val username = WikalloyConfig.getMySQLUsername
    val password = WikalloyConfig.getMySQLPassword
    // connect to the database instance
    val mr = Using(DriverManager.getConnection(jdbcUrl, username, password)) { conn =>
      // create a database for this test
      val dbname = f"WIKALLOY${System.currentTimeMillis()}%010d"
      conn.createStatement().execute(s"CREATE DATABASE $dbname")
      conn.setCatalog(dbname)

      // create the tables
      val begCreate = System.currentTimeMillis()
      impl.getCreateQueries.asScala.foreach(q => conn.createStatement().execute(q))
      val endCreate = System.currentTimeMillis()

      // run the concrete load on the database
      val insertTimes = new ListBuffer[Long]
      val selectTimes = new ListBuffer[Long]
      val storageSize = new ListBuffer[Double]
      for (cl <- impl.getConcreteLoads.asScala) {
        // time the inserts for this load
        val begInsert = System.currentTimeMillis()
        cl.getInsertQueries.asScala.foreach { q =>
          try {
            conn.createStatement().execute(q)
          } catch {
            case NonFatal(e) => logger.warn(s"Error executing insert query: ${e.getMessage}\n   Query: $q")
          }
        }
        val endInsert = System.currentTimeMillis()
        insertTimes += (endInsert - begInsert)

        // time the selects for this load
        val begSelect = System.currentTimeMillis()
        cl.getSelectQueries.asScala.foreach { q =>
          try {
            conn.createStatement().execute(q)
          } catch {
            case NonFatal(e) => logger.warn(s"[${cl.getUUID}] Error executing select query: ${e.getMessage}\n   Query: $q")
          }
        }
        val endSelect = System.currentTimeMillis()
        selectTimes += (endSelect - begSelect)

        // get the storage space
        val ss = conn.createStatement().executeQuery(
          s"select table_schema, sum((data_length+index_length)/1024) AS KB from information_schema.tables where table_schema='$dbname' group by 1;"
        )
        if (ss.next()) {
          storageSize += ss.getDouble("KB")
        }

        // clear all the tables in the database for the next load
        val rs = conn.createStatement().executeQuery(
          s"SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='$dbname'"
        )
        while (rs.next()) {
          val tableName = rs.getString("TABLE_NAME")
          conn.createStatement().execute(s"TRUNCATE TABLE `$dbname`.`$tableName`")
        }
        logger.debug(s"[$solution][${cl.getUUID}] concrete load tested.")
      }
      // drop the database
      conn.createStatement().execute(s"DROP DATABASE $dbname")
      // create the result for this database design
      val insertTimesList = insertTimes.toList
      val selectTimesList = selectTimes.toList
      val storageSizeList = storageSize.toList
      logger.info(s"[$solution] concrete load testing finished.")
      new MeasurementResult(
        solution.toString,
        endCreate - begCreate,
        insertTimesList.sum.toDouble,
        selectTimesList.sum.toDouble,
        storageSizeList.sum)
    }.getOrElse(new MeasurementResult(solution.getFileName.toString, -1, -1, -1, -1))
    mr
  }
}
