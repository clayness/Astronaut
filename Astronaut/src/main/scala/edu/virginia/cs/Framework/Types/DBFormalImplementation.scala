package edu.virginia.cs.Framework.Types

import edu.virginia.cs.Synthesizer.Sig

import java.util

class DBFormalImplementation {
  private var formalImplementation: String = ""
  private var sigs: util.List[Sig] = _
  private var ids: util.List[String] = new util.ArrayList[String]()
  private var associationsForCreateSchemas: util.List[String] = _
  private var typeMap: util.Map[String, String] = _
  
  def getAssociationsForCreateSchemas: util.List[String] = {
    this.associationsForCreateSchemas
  }

  def setAssociationsForCreateSchemas(list: util.List[String]): Unit = {
    this.associationsForCreateSchemas = list
  }

  def getTypeMap: util.Map[String, String] = {
    this.typeMap
  }

  def setTypeMap(hm: util.Map[String, String]): Unit = {
    this.typeMap = hm
  }

  def getSigs: util.List[Sig] = {
    this.sigs
  }

  def setSigs(sigs: util.List[Sig]): Unit = {
    this.sigs = sigs
  }

  def getIds: util.List[String] = {
    this.ids
  }

  def setIds(list: util.List[String]): Unit = {
    this.ids = list
  }

  def getImplementation: String = formalImplementation

  def setImp(imp: String): Unit = {
    this.formalImplementation = imp
  }
}
