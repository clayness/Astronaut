package edu.virginia.cs.Framework.Types

import edu.virginia.cs.Synthesizer.Sig

import java.util

// This class must be Scala class to be pluged into the framework
class DBFormalImplementation {
  private var formalImplementation: String = ""

  // Chong: these members are really need to be in DBImplementation
  // when create schemas, these members can be filled with values

  private var sigs: java.util.ArrayList[Sig] = _
  private var ids: java.util.ArrayList[String] = new java.util.ArrayList[String]()
  private var associationsForCreateSchemas: java.util.ArrayList[String] = _
  private var typeMap: java.util.HashMap[String, String] = _

  def getAssociationsForCreateSchemas: util.ArrayList[String] = {
    this.associationsForCreateSchemas
  }

  def getTypeMap: java.util.HashMap[String, String] = {
    this.typeMap
  }

  def setIds(list: java.util.ArrayList[String]): Unit = {
    this.ids = list
  }

  def setAssociationsForCreateSchemas(list: java.util.ArrayList[String]): Unit = {
    this.associationsForCreateSchemas = list
  }

  def setTypeMap(hm: java.util.HashMap[String, String]): Unit = {
    this.typeMap = hm
  }

  def getSigs: java.util.ArrayList[Sig] = {
    this.sigs
  }

  def setSigs(sigs: java.util.ArrayList[Sig]): Unit = {
    this.sigs = sigs
  }

  def getIds: java.util.ArrayList[String] = {
    this.ids
  }

  // store benchmark path to innerValue
  def getImplementation: String = formalImplementation

  def setImp(imp: String): Unit = {
    this.formalImplementation = imp
  }

}
