package edu.virginia.cs

import java.io._
import java.util
import scala.jdk.CollectionConverters._

/**
 * @author tang
 */


// This is the Main class, the entry point of the DB instance of Trademaker Framework
object Main {
  var isDebugOn: Boolean = AppConfig.getDebug

  def main(args: Array[String]): Unit = {

    // first get database
    val testdb: String = args(0)
    if (testdb.toLowerCase == "mysql" || testdb.toLowerCase == "postgres") {
      AppConfig.setTestDB(testdb.toLowerCase.trim)
      println("In Main: " + AppConfig.getTestDB)
    } else {
      println("Main: Non supported RDBMS...")
      return
    }

    // if specification is give by arguments,
    // we will use it instead of those set in configuration file
    val specs: Array[String] = args.slice(1, args.length)
    if (specs.length > 0) {
      val specList = new util.ArrayList[String]()
      for (spec <- specs) {
        // get full path of the spec file
        val specPath = new File(spec).getAbsolutePath
        specList.add(specPath)
      }
      AppConfig.setSpecList(specList.asScala.toList)
    }

    val myDBTrademaker = new DBTrademaker()
    myDBTrademaker.run()

    if (isDebugOn) {
      println("Done")
    }
  }
}