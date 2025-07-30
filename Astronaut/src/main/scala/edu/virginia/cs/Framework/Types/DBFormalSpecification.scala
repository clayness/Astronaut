package edu.virginia.cs.Framework.Types

import edu.virginia.cs.Synthesizer.{AlloyOMToAlloyDM, Sig}

import java.util

/**
 * Represents a formal specification for database structures using Alloy specifications.
 * This class handles parsing and storing of database model specifications including
 * identifiers, associations, type mappings, and signatures.
 *
 * @param specPath The file path to the Alloy specification file
 */
class DBFormalSpecification(specPath: String) {
  private var ids: util.List[String] = new util.ArrayList[String]()
  private var associations: util.List[String] = _
  private var typeMap: util.Map[String, String] = _
  private var sigs: util.List[Sig] = _

  /**
   * Retrieves the specification file path.
   *
   * @return The path to the Alloy specification file as a String
   */
  def getSpec: String = specPath

  /**
   * Parses the Alloy specification file and initializes the internal data structures.
   * Converts the Object Model to Data Model using AlloyOMToAlloyDM converter.
   * Sets up IDs, associations, type mappings, and signatures for the database model.
   *
   * The method:
   * 1. Generates the data model file path by replacing the extension
   * 2. Uses a fixed scope of 6 for integer values
   * 3. Populates internal lists and maps with parsed data
   */
  def parseSpec(): Unit = {
    val objSpecPath = specPath.substring(0, specPath.length() - 4) + "_dm.als"
    val intScope = 6

    val aotad: AlloyOMToAlloyDM = new AlloyOMToAlloyDM()
    aotad.run(specPath, objSpecPath, intScope)
    this.ids = aotad.getIDs
    this.associations = aotad.getAss
    this.typeMap = aotad.getTypeList
    this.sigs = aotad.getSigs
  }

  /**
   * Gets the list of identifiers parsed from the specification.
   *
   * @return A List of Strings containing all identifiers in the specification
   */
  def getIds: util.List[String] = {
    this.ids
  }

  /**
   * Gets the list of associations between entities in the specification.
   *
   * @return A List of Strings containing all associations defined in the specification
   */
  def getAssociations: util.List[String] = {
    this.associations
  }

  /**
   * Gets the type mapping for all elements in the specification.
   * The map links field names to their corresponding data types.
   *
   * @return A Map with String keys (field names) and String values (data types)
   */
  def getTypeMap: util.Map[String, String] = {
    this.typeMap
  }

  /**
   * Gets the list of signatures (Sigs) defined in the specification.
   * Signatures represent the structural elements of the database model.
   *
   * @return A List of Sig objects representing all signatures in the specification
   */
  def getSigs: util.List[Sig] = {
    this.sigs
  }
}