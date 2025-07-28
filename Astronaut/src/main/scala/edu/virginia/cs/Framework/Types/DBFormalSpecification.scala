package edu.virginia.cs.Framework.Types

/**
 * Created by tang on 8/8/14.
 */

import edu.virginia.cs.Synthesizer.{AlloyOMToAlloyDM, Sig}

import java.util

class DBFormalSpecification(specPath: String) { //extends FrameworkTypeWrapper {
  //override type WrapperType = String
  //  private var specPath: String = ""

  private var ids: util.ArrayList[String] = new util.ArrayList[String]()
  private var associations: util.ArrayList[String] = _
  private var typeMap: util.HashMap[String, String] = _
  private var sigs: util.ArrayList[Sig] = _

  // store benchmark path to innerValue
  //makeWrapper(path)

  //  def setSpec(path: String) = {
  //    specPath = path
  //  }

  def getSpec: String = specPath

  def parseSpec(): Unit = {
    // Chong: in order to fill in these members, and call legacy code, 
    // I have to create AlloyDM here, and in this procedure, fill those information

    //    var fSpecPath = specPath.asInstanceOf[DBFormalSpecification].getSpec
    val objSpecPath = specPath.substring(0, specPath.length() - 4) + "_dm.als"
    val intScope = 6

    val aotad: AlloyOMToAlloyDM = new AlloyOMToAlloyDM()
    // by calling run, (legacy) Object Specification will be created
    aotad.run(specPath, objSpecPath, intScope)
    this.ids = aotad.getIDs
    this.associations = aotad.getAss
    this.typeMap = aotad.getTypeList
    this.sigs = aotad.getSigs
  }

  def getIds: util.ArrayList[String] = {
    this.ids
  }

  def getAssociations: util.ArrayList[String] = {
    this.associations
  }

  def getTypeMap: util.HashMap[String, String] = {
    this.typeMap
  }

  def getSigs: util.ArrayList[Sig] = {
    this.sigs
  }
}
