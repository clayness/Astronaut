package analysis

import edu.virginia.cs.AppConfig
import edu.virginia.cs.Framework.AstronautFramework
import edu.virginia.cs.Framework.Types._
import edu.virginia.cs.Synthesizer.{CodeNamePair, PrintOrder}
import org.apache.spark.{SparkConf, SparkContext}

import java.io.{File, FileWriter, PrintWriter}
import java.util
import scala.jdk.CollectionConverters._

class SparkAnalyzer extends AstronautFramework with Serializable {

  private val queryGenerator: QueryGenerator = new QueryGenerator()

  def analyze(list: List[(ImplementationType, MeasurementFunctionSetType)]): List[(ImplementationType, MeasurementResultSetType)] = {
    val conf = new SparkConf().setAppName("Astronaut")
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
    val rdd = sc.parallelize(list)
    // map the RDD across the Spark cluster, running the analysis
    // function on each pair
    val evaluationResult = rdd.map(e => computeMetricsForImpl(e._1, e._2))
    val collectedResult = evaluationResult.collect()

    sc.stop()
    collectedResult.toList
  }

  def computeMetricsForImpl(impl: ImplementationType, mfs: MeasurementFunctionSetType): (ImplementationType, MeasurementResultSetType) = {
    // If benchmark is random test loads, need to create concrete load first and then run them
    if (AppConfig.getIsRandom == 1) {
      // call concrete loads creator here
      val timeLoads: util.List[ConcreteLoad] = new util.ArrayList[ConcreteLoad](2)
      val spaceLoads: util.List[ConcreteLoad] = new util.ArrayList[ConcreteLoad](1)
      val insertLoad: ConcreteLoad = generateRandomInsertStatements(impl, mfs.getCtmf.getInstances)
      val selectLoad: ConcreteLoad = generateRandomSelectStatements(impl, mfs.getCtmf.getInstances)
      timeLoads.add(insertLoad)
      timeLoads.add(selectLoad)

      spaceLoads.add(insertLoad)
      mfs.getCtmf.setLoads(timeLoads)
      mfs.getCsmf.setLoads(spaceLoads)
    }

    val tmf = mfs.getCtmf
    tmf.setImpl(impl)
    val smf = mfs.getCsmf
    smf.setImpl(impl)
    val tmr = tmf.run()
    println("Insert time: " + tmr.getInsertTime + "s. Select time: " + tmr.getSelectTime + "s.")
    val smr = smf.run()
    val dbmr = new DBMeasurementResult(tmr, smr)
    dbmr.setImpl(impl)

    // delete insert and select files
    val loads = mfs.getCtmf.getLoads.asScala
    for (load <- loads) {
      val inPath = load.getInsertPath
      val slPath = load.getSelectPath

      val inFile = new File(inPath)
      if (inFile.exists()) {
        inFile.delete()
      }
      val slFile = new File(slPath)
      if (slFile.exists()) {
        slFile.delete()
      }
    }
    (new DBImplementation(impl.getImPath), dbmr)
  }

  // get the random instances, and return insertFilePath
  private def generateRandomInsertStatements(impl: DBImplementation, instances: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): ConcreteLoad = {
    val insertSpecializedQuery: SpecializedQuery = queryGenerator.specializeInsertQuery(null, impl, instances)
    val cq = new ConcreteQuery()
    cq.setSq(insertSpecializedQuery)
    val cqs = new util.ArrayList[ConcreteQuery](1)
    cqs.add(cq)
    val
    insCL = new ConcreteLoad()
    insCL.setQuerySet(cqs)

    val implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    val implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs()
    }
    val insertPath = pathBase + File.separator + implFileName + "_insert.sql"
    insCL.setInsertPath(insertPath)
    insCL.setSelectPath("")
    val insertFile: File = new File(insertPath)
    if (!insertFile.exists()) {
      insertFile.createNewFile()
    }

    val insertPw: PrintWriter = new PrintWriter(new FileWriter(insertPath, true))
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      insertPw.println("USE " + implFileName + ";")
    } else if (AppConfig.getTestDB.equalsIgnoreCase("postgres")) {
      insertPw.println("BEGIN;")
    }
    val allInsertStmts = new util.ArrayList[util.Map[String, util.Map[Integer, String]]]().asScala
    for (elem <- insCL.getQuerySet.asScala) {
      val sq = elem.getSq
      val sqInOneObject = sq.getInsertStmtsInOneObject
      allInsertStmts.asJava.add(sqInOneObject)
    }

    val printOrder = PrintOrder.getOutPutOrders(pathBase).asScala
    for (s <- printOrder) {
      for (insertS <- allInsertStmts) {
        for (table <- insertS.asScala) {
          if (table._1.equalsIgnoreCase(s)) { // check table name
            val stmt = table._2
            for (idStmt <- stmt.asScala) {
              val stmtStr: String = String.valueOf(idStmt._2.toCharArray)
              insertPw.println(stmtStr)
            }
            insertPw.flush()
          }
        }
      }
    }

    insCL.setInsertPath(insertPath)
    if (AppConfig.getTestDB.equalsIgnoreCase("postgres")) {
      insertPw.println("COMMIT;")
    }
    insertPw.flush()
    insertPw.close()

    insCL.getQuerySet.clear()
    insertSpecializedQuery.getInsertStmtsInOneObject.clear()
    insertSpecializedQuery.getSelectStmtsInOneObject.clear()

    insCL
  }

  // get the random instances, and return insertFilePath
  private def generateRandomSelectStatements(impl: DBImplementation, instances: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): ConcreteLoad = {
    val selectSpecializedQuery: SpecializedQuery = queryGenerator.specializeSelectQuery(null, impl, instances)

    val cq = new ConcreteQuery()
    cq.setSq(selectSpecializedQuery)
    val cqs = new util.ArrayList[ConcreteQuery]()
    cqs.add(cq)
    val selCL = new ConcreteLoad()
    selCL.setQuerySet(cqs)

    // convert select statements
    val implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    val implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs()
    }
    val printOrder = PrintOrder.getOutPutOrders(pathBase).asScala

    val selectPath = pathBase + File.separator + implFileName + "_select.sql"
    //    val compressedSelectPath = pathBase + File.separator + implFileName + "_select.sql.tar.gz"
    //    var tmpSelectPath = pathBase + File.separator + implFileName + "_select_tmp.sql"
    selCL.setSelectPath(selectPath)
    selCL.setInsertPath("")
    val selectFile: File = new File(selectPath)
    //    var tmpSelectFile: File = new File(tmpSelectPath)
    if (!selectFile.exists()) {
      selectFile.createNewFile()
    }

    val selectPw: PrintWriter = new PrintWriter(new FileWriter(selectPath, true))
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      selectPw.println("USE " + implFileName + ";")
    }
    val allSelectStmts = new util.ArrayList[util.Map[String, util.Map[Integer, String]]]().asScala

    for (elem <- selCL.getQuerySet.asScala) {
      val sq = elem.getSq
      val sqInOneObject = sq.getSelectStmtsInOneObject
      allSelectStmts.asJava.add(sqInOneObject)
    }

    for (s: String <- printOrder) {
      for (selectS <- allSelectStmts) {
        for (elem <- selectS.asScala) {
          if (elem._1.equalsIgnoreCase(s)) {
            val tmp = elem._2
            for (stmt <- tmp.asScala) {
              val stmtStr = String.valueOf(stmt._2.toCharArray)
              selectPw.println(stmtStr)
            }
            selectPw.flush()
          }
        }
      }
    }

    selCL.setSelectPath(selectPath)
    selectPw.flush()
    selectPw.close()

    /**
     * // compress test cases and delete sql file
     * Process(Seq("tar", "czf", compressedSelectPath, "-C", pathBase, implFileName + "_select.sql")).!
     * Process(Seq("rm", selectPath)).!
     */
    selCL.getQuerySet.clear()
    selectSpecializedQuery.getInsertStmtsInOneObject.clear()
    selectSpecializedQuery.getSelectStmtsInOneObject.clear()

    selCL
  }
}
