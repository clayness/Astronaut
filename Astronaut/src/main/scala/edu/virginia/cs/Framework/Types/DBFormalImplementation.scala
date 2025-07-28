package edu.virginia.cs.Framework.Types

import edu.virginia.cs.Synthesizer.Sig

import java.util

// This class must be Scala class to be pluged into the framework
class DBFormalImplementation { // extends FrameworkTypeWrapper {
  //  override type WrapperType = String
  private var formalImplementation: String = ""

  // Chong: these members are really need to be in DBImplementation
  // when create schemas, these members can be filled with values

  private var sigs: util.ArrayList[Sig] = _
  private var ids: util.ArrayList[String] = new util.ArrayList[String]()
  private var associationsForCreateSchemas: util.ArrayList[String] = _
  private var typeMap: util.HashMap[String, String] = _


  def getAssociationsForCreateSchemas: util.ArrayList[String] = {
    this.associationsForCreateSchemas
  }

  def getTypeMap: util.HashMap[String, String] = {
    this.typeMap
  }

  def setIds(list: util.ArrayList[String]): Unit = {
    this.ids = list
  }

  def setAssociationsForCreateSchemas(list: util.ArrayList[String]): Unit = {
    this.associationsForCreateSchemas = list
  }

  def setTypeMap(hm: util.HashMap[String, String]): Unit = {
    this.typeMap = hm
  }

  def getSigs: util.ArrayList[Sig] = {
    this.sigs
  }

  def setSigs(sigs: util.ArrayList[Sig]): Unit = {
    this.sigs = sigs
  }


  def getIds: util.ArrayList[String] = {
    this.ids
  }


  // store benchmark path to innerValue
  //  makeWrapper(solutionPath)
  def getImplementation: String = formalImplementation

  def setImp(imp: String): Unit = {
    this.formalImplementation = imp
  }

  /**
   * Need to set all needed information for test cases generation here
   */
  //    schemas.put(fImpFileName, parser.getDataSchemas());
  //    parents.put(fImpFileName, parser.getParents());
  //    reverseTAss.put(fImpFileName, parser.getReverseTAssociate());
  //    foreignKeys.put(fImpFileName, parser.getForeignKey());
  //    association.put(fImpFileName, parser.getAssociation());
  //    primaryKeys.put(fImpFileName, parser.getPrimaryKeys());
  //    fields.put(fImpFileName, parser.getFields());
  //    allFields.put(fImpFileName, parser.getallFields());
  //    fieldsTable.put(fImpFileName, parser.getFieldsTable());
  //    fieldType.put(fImpFileName, parser.getFieldType());
}
