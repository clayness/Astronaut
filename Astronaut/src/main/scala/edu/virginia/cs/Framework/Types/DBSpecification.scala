package edu.virginia.cs.Framework.Types

import scala.io.Source

class DBSpecification(specFile: String) {

  def getSpecFile: String = this.specFile

  def getFileContent: String = {
    val src = Source.fromFile(this.specFile)
    try {
      src.getLines().mkString
    } finally {
      src.close()
    }
  }

}
