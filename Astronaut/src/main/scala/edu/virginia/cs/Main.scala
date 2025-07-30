package edu.virginia.cs

import java.io._
import java.util
import scala.jdk.CollectionConverters._

// entry point of the TradeMaker framework
object Main {

  def main(args: Array[String]): Unit = {
    val testdb: String = args(0)
    if (testdb.toLowerCase == "mysql" || testdb.toLowerCase == "postgres") {
      AppConfig.setTestDB(testdb.toLowerCase.trim)
    } else {
      printf("Error: Non supported RDBMS [%s]%n", testdb)
      return
    }

    val specs: Array[String] = args.slice(1, args.length)
    if (specs.length > 0) {
      val specList = new util.ArrayList[String]()
      for (spec <- specs) {
        specList.add(new File(spec).getAbsolutePath)
      }
      AppConfig.setSpecList(specList.asScala.toList)
    }

    new DBTrademaker().run()
  }
}