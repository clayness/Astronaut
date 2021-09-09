package edu.virginia.cs

import java.io._
import scala.collection.JavaConverters._

// This is the Main class, the entry point of the DB instance of Trademaker Framework
object Main {

  def main(args: Array[String]): Unit = {
    // first get database
    val testDb: String = args(0).toLowerCase.trim
    testDb match {
      case "mysql" | "postgres" => AppConfig.setTestDB(testDb)
      case _ =>
        println("Main: Non supported RDBMS...")
        return
    }

    // if specification is give by arguments,
    // we will use it instead of those set in configuration file
    val specList = args.drop(1).map(new File(_).getAbsolutePath)
    if (specList.length > 0)
      AppConfig.setSpecList(specList.toList.asJava)
    new DBTrademaker().run()
  }
}