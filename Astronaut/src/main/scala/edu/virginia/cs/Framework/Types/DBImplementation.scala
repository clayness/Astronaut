package edu.virginia.cs.Framework.Types

import scala.io.Source
import edu.virginia.cs.Synthesizer.CodeNamePair

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.DocumentBuilder
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.w3c.dom.Node
import edu.virginia.cs.Synthesizer.Sig

import scala.collection.JavaConversions._
import edu.virginia.cs.Synthesizer.DataProvider

import java.io.Serializable

/**
 * Created by tang on 8/9/14.
 */

// ImpelementationType here will be the file path of SQL schema script

class DBImplementation(path:String) extends Serializable { //extends FrameworkTypeWrapper {

  private val implPath: String = path

  private var dataProvider: DataProvider = _
  private var reverseTAssociate: java.util.ArrayList[CodeNamePair] = _
  private var foreignKeys: java.util.ArrayList[CodeNamePair] = _
  // HashMap[Association Name, pair[src, dst], src and dst are class name
  private var associations: java.util.HashMap[String, CodeNamePair] = _
  private var primaryKeys: java.util.ArrayList[CodeNamePair] = _
  private var fields: java.util.ArrayList[CodeNamePair] = _
  private var allFields: java.util.ArrayList[String] = _
  private var fieldsTable: java.util.ArrayList[CodeNamePair] = _


  // this the reverse of "id" in implementation
  private var reverseIDs: java.util.ArrayList[CodeNamePair] = _

  private var sigs: java.util.ArrayList[Sig] = _
  private var ids: java.util.ArrayList[String] = _
  private var associationsForCreateSchemas: java.util.ArrayList[String] = _
  private var typeMap: java.util.HashMap[String, String] = _

  def setSigs(sigs: java.util.ArrayList[Sig]): Unit = {
    this.sigs = sigs
  }

  def getIds: java.util.ArrayList[String] = {
    this.ids
  }

  def setIds(ids: java.util.ArrayList[String]): Unit = {
    this.ids = ids
  }

  def getAssociationsForCreateSchemas: java.util.ArrayList[String] = {
    this.associationsForCreateSchemas
  }

  def setAssociationsForCreateSchemas(asss: java.util.ArrayList[String]): Unit = {
    this.associationsForCreateSchemas = asss
  }

  def getTypeMap: java.util.HashMap[String, String] = {
    this.typeMap
  }

  def setTypeMap(typeMap: java.util.HashMap[String, String]): Unit = {
    this.typeMap = typeMap
  }

  // store benchmark path to innerValue
  //makeWrapper(implementationPath)

  // get file content, file path can be retrieved by calling getInnerValue()
  def getFileContent:String = {
    val src = Source.fromFile(implPath)
    try {
      src.getLines().mkString
    } finally {
      src.close()
    }
  }

  def getImPath:String = implPath

  def getDataProvider: DataProvider = {
    this.dataProvider
  }

  def setDataProvider(dp: DataProvider): Unit = {
    this.dataProvider = dp
  }

  def getReverseTAssociate: java.util.ArrayList[CodeNamePair] = {
    this.reverseTAssociate 
  }

  def setReverseTAssociate(rTAss: java.util.ArrayList[CodeNamePair]): Unit = {
    this.reverseTAssociate = rTAss
  }

  def getForeignKeys: java.util.ArrayList[CodeNamePair] = {
    this.foreignKeys 
  }

  def setForeignKeys(fKeys: java.util.ArrayList[CodeNamePair]): Unit = {
    this.foreignKeys = fKeys
  }

  def getAssociations: java.util.HashMap[String, CodeNamePair] = {
    this.associations 
  }

  def setAssociations(ass: java.util.HashMap[String, CodeNamePair]): Unit = {
    this.associations = ass
  }

  def getPrimaryKeys: java.util.ArrayList[CodeNamePair] = {
    this.primaryKeys 
  }

  def setPrimaryKeys(pKeys: java.util.ArrayList[CodeNamePair]): Unit = {
    this.primaryKeys = pKeys
  }

  def getFields: java.util.ArrayList[CodeNamePair] = {
    this.fields 
  }

  def setFields(fields: java.util.ArrayList[CodeNamePair]): Unit = {
    this.fields = fields
  }

  def getAllFields: java.util.ArrayList[String] = {
    this.allFields 
  }

  def setAllFields(af: java.util.ArrayList[String]): Unit = {
    this.allFields = af
  }

  def getFieldsTable: java.util.ArrayList[CodeNamePair] = {
    this.fieldsTable 
  }

  def setFieldsTable(ft: java.util.ArrayList[CodeNamePair]): Unit = {
    this.fieldsTable = ft
  }

  def getReverseIDs: java.util.ArrayList[CodeNamePair] = {
    this.reverseIDs
  }

  def setReverseIDs(ids:java.util.ArrayList[CodeNamePair]): Unit = {
    this.reverseIDs = ids
  }

  def getSigs: java.util.ArrayList[Sig] = {
    this.sigs
  }

  // parse the xml file and store all information back to data structures
  def parseImplXMLFile(): Unit = {
    val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    try {
      //Using factory get an instance of document builder
      val db: DocumentBuilder = dbf.newDocumentBuilder()
      //parse using builder to get DOM representation of the XML file
      val dom: Document = db.parse(this.implPath)

      //get the root element
      val docEle: Element = dom.getDocumentElement
      //get a nodelist of elements
      val fieldnodes: NodeList = docEle.getElementsByTagName("field")
      // handle parent first
      for (i <- 0 to fieldnodes.getLength) {
        val node: Node = fieldnodes.item(i)
        if (node.hasAttributes) {
          val element: Element = node.asInstanceOf[Element]
          val labelValue: String = element.getAttribute("label")
          if (labelValue.equalsIgnoreCase("parent")) {
            parseParent(element)
          }
        }
      }

      // handle other labels
      for (i <- 0 to fieldnodes.getLength) {
        val node: Node = fieldnodes.item(i)
        // find different sub nodes based on label value
        // find node if the "label" attribute is "primaryKey"
        if (node.hasAttributes) {
          val element: Element = node.asInstanceOf[Element]
          val labelValue: String = element.getAttribute("label")
          if (labelValue.equalsIgnoreCase("primarykey")) {
            parsePK(element)
          } else if (labelValue.equalsIgnoreCase("fields")) {
            parseFields(element)
          } else if (labelValue.equalsIgnoreCase("foreignKey")) {
            parseFK(element)
          } else if (labelValue.equalsIgnoreCase("tAssociate")) {
            parse_tAssociate(element)
          } else if (labelValue.equalsIgnoreCase("fAssociate")) {
            parse_fAssociate(element)
          } else if (labelValue.equalsIgnoreCase("attrSet")) {
            parseAttrSet(element)
          } else if (labelValue.equalsIgnoreCase("id")) {
            parseId(element)
          } else if (labelValue.equalsIgnoreCase("src")) {
            parseSrc(element)
          } else if (labelValue.equalsIgnoreCase("dst")) {
            parseDst(element)
          } else if (labelValue.equalsIgnoreCase("src_multiplicity")) {
            parseSrcMultiplicity(element)
          } else if (labelValue.equalsIgnoreCase("dst_multiplicity")) {
            parseDstMultiplicity(element)
          }
        }
      }

      // get sig nodes
      val signodes: NodeList = docEle.getElementsByTagName("sig")
      for (i <- 0 to signodes.getLength) {
        val node: Node = signodes.item(i)
        // find different subnodes based on label value
        // find node if the "label" attribute is "primaryKey"
        if (node.hasAttributes) {
          val element = node.asInstanceOf[Element]
          val labelValue = element.getAttribute("label")
          val tmp = labelValue.split("/")
          val `type` = tmp(tmp.length - 1)
          if (`type`.equalsIgnoreCase("Real") || `type`.equalsIgnoreCase("Integer") ||
            `type`.equalsIgnoreCase("string") ||
            `type`.equalsIgnoreCase("Class") ||
            `type`.equalsIgnoreCase("DType") ||
            `type`.equalsIgnoreCase("Bool") ||
            `type`.equalsIgnoreCase("Longblob") ||
            `type`.equalsIgnoreCase("Time")) {
            val atoms = element.getElementsByTagName("atom")
            for (j <- 0 until atoms.getLength) {
              val node1 = atoms.item(j)
              val tableCode = node1.asInstanceOf[Element]
              val code = tableCode.getAttribute("label")
              val atomLabel = code.split("/")
              val name = atomLabel(atomLabel.length - 1)
              this.dataProvider.addType(name, `type`)
            }
          }
        }
      }
    } catch {
      case pce: Exception => pce.printStackTrace()
    }
  }

  def parse_tAssociate(element: Element): Unit = {
    var children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val code = getSingleAtom(singleTuple, 0)
        var name = getSingleAtom(singleTuple, 1)
        val hasCode = this.dataProvider.hasPairCode(code)
        if (hasCode) {
          val root = getRootTable(name)
          name = root
          this.dataProvider.removePairByCode(code)
        }
        this.dataProvider.addPair(code, name)
      }
    }
    children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val code = getSingleAtom(singleTuple, 0)
        val name = getSingleAtom(singleTuple, 1)
        val tableName = this.dataProvider.getSecondByFirst(code)
        this.reverseTAssociate.add(new CodeNamePair(name, tableName))
      }
    }
  }

  def parsePK(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val key = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "primaryKey", key)
        this.primaryKeys.add(new CodeNamePair(table, key))
      }
    }
  }

  def parseFK(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val key = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "foreignKey", key)
        this.foreignKeys.add(new CodeNamePair(table, key))
      }
    }
  }

  def parseFields(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val field = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "fields", field)
        this.fields.add(new CodeNamePair(table, field))
        this.allFields.add(field)
        this.fieldsTable.add(new CodeNamePair(field, table))
      }
    }
  }

  def parse_fAssociate(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val code = getSingleAtom(singleTuple, 0)
        val name = getSingleAtom(singleTuple, 1)
        this.dataProvider.addPair(code, name)
      }
    }
  }

  def parseId(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val Id = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "Id", Id)
      }
    }
  }

  def parseParent(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val childTable = getSingleAtom(singleTuple, 0)
        val parentTable = getSingleAtom(singleTuple, 1)
        this.dataProvider.addParent(childTable, parentTable)
      }
    }
  }

  def parseSrc(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val srcTable = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "src", srcTable)
        if (this.associations.containsKey(table)) {
          setAssociation(table, "src", srcTable)
        } else {
          this.associations.put(table, new CodeNamePair(srcTable, ""))
        }
      }
    }
  }

  def parseDst(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val dstTable = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "dst", dstTable)
        if (this.associations.containsKey(table)) {
          setAssociation(table, "dst", dstTable)
        } else {
          this.associations.put(table, new CodeNamePair("", dstTable))
        }
      }
    }
  }

  def parseSrcMultiplicity(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val parentTable = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "srcMul", parentTable)
      }
    }
  }

  def parseDstMultiplicity(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val parentTable = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "dstMul", parentTable)
      }
    }
  }

  def parseAttrSet(element: Element): Unit = {
    val children = element.getElementsByTagName("tuple")
    for (j <- 0 until children.getLength) {
      val child = children.item(j)
      if (child.hasChildNodes) {
        val singleTuple = child.asInstanceOf[Element]
        val table = getSingleAtom(singleTuple, 0)
        val attr = getSingleAtom(singleTuple, 1)
        this.dataProvider.addItem(table, "attr", attr)
      }
    }
  }

  def getSingleAtom(singleTuple: Element, i: Int): String = {
    val atoms = singleTuple.getElementsByTagName("atom")
    val tableCode = atoms.item(i).asInstanceOf[Element]
    var code = tableCode.getAttribute("label")
    val tmp = code.split("/")
    code = tmp(tmp.length - 1)
    code
  }

  // chong: maybe elem._2.setFirst() cannot set the value to associations map
  def setAssociation(assName: String, target: String, value: String): Unit = {
    // iterate java hashmap
    val it = this.associations.iterator
    while (it.hasNext) {
      val elem = it.next
      val key = elem._1
      if (key.equalsIgnoreCase(assName)) {
        if (target.equalsIgnoreCase("src")) {
          elem._2.setFirst(value)
        } else if (target.equalsIgnoreCase("dst")) {
          elem._2.setSecond(value)
        }
      }
    }
  }

  def getRootTable(tableName: String): String = {
    for (i <- 0 to this.sigs.size()) {
      val sig = this.sigs.get(i)
      if (sig.getSigName.equalsIgnoreCase(tableName)) {
        if (!sig.isHasParent) {
          return sig.getSigName
        } else {
          return getRootTable(sig.getParent)
        }
      }
    }
    null
  }
  
  def getDataSchemas: java.util.HashMap[String, java.util.ArrayList[CodeNamePair]] = {
    this.dataProvider.getTables
  }
}
