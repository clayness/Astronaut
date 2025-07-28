package edu.virginia.cs.Framework.Types

import edu.virginia.cs.Synthesizer.{CodeNamePair, DataProvider, Sig}
import org.w3c.dom.Element

import java.util
import scala.jdk.CollectionConverters._

/**
 * Created by tang on 8/9/14.
 */

// ImpelementationType here will be the file path of SQL schema script

class DBImplementation(path: String) extends Serializable { //extends FrameworkTypeWrapper {

  private val implPath: String = path

  private var dataProvider: DataProvider = _
  private var reverseTAssociate: util.ArrayList[CodeNamePair] = _
  private var foreignKeys: util.ArrayList[CodeNamePair] = _
  // HashMap[Association Name, pair[src, dst], src and dst are class name
  private var associations: util.HashMap[String, CodeNamePair] = _
  private var primaryKeys: util.ArrayList[CodeNamePair] = _
  private var fields: util.ArrayList[CodeNamePair] = _
  private var allFields: util.ArrayList[String] = _
  private var fieldsTable: util.ArrayList[CodeNamePair] = _


  // this the reverse of "id" in implementation
  private var reverseIDs: util.ArrayList[CodeNamePair] = _

  private var sigs: util.ArrayList[Sig] = _
  private var ids: util.ArrayList[String] = _
  private var associationsForCreateSchemas: util.ArrayList[String] = _
  private var typeMap: util.HashMap[String, String] = _

  def setSigs(sigs: util.ArrayList[Sig]): Unit = {
    this.sigs = sigs
  }

  def getIds: util.ArrayList[String] = {
    this.ids
  }

  def setIds(ids: util.ArrayList[String]): Unit = {
    this.ids = ids
  }

  def getAssociationsForCreateSchemas: util.ArrayList[String] = {
    this.associationsForCreateSchemas
  }

  def setAssociationsForCreateSchemas(asss: util.ArrayList[String]): Unit = {
    this.associationsForCreateSchemas = asss
  }

  def getTypeMap: util.HashMap[String, String] = {
    this.typeMap
  }

  def setTypeMap(typeMap: util.HashMap[String, String]): Unit = {
    this.typeMap = typeMap
  }

  // store benchmark path to innerValue
  //makeWrapper(implementationPath)

  def getImPath: String = implPath

  def getDataProvider: DataProvider = {
    this.dataProvider
  }

  def setDataProvider(dp: DataProvider): Unit = {
    this.dataProvider = dp
  }

  def getReverseTAssociate: util.ArrayList[CodeNamePair] = {
    this.reverseTAssociate
  }

  def setReverseTAssociate(rTAss: util.ArrayList[CodeNamePair]): Unit = {
    this.reverseTAssociate = rTAss
  }

  def setForeignKeys(fKeys: util.ArrayList[CodeNamePair]): Unit = {
    this.foreignKeys = fKeys
  }

  def getAssociations: util.HashMap[String, CodeNamePair] = {
    this.associations
  }

  def setAssociations(ass: util.HashMap[String, CodeNamePair]): Unit = {
    this.associations = ass
  }

  def getPrimaryKeys: util.ArrayList[CodeNamePair] = {
    this.primaryKeys
  }

  def setPrimaryKeys(pKeys: util.ArrayList[CodeNamePair]): Unit = {
    this.primaryKeys = pKeys
  }

  def getFields: util.ArrayList[CodeNamePair] = {
    this.fields
  }

  def setFields(fields: util.ArrayList[CodeNamePair]): Unit = {
    this.fields = fields
  }

  def setAllFields(af: util.ArrayList[String]): Unit = {
    this.allFields = af
  }

  def getFieldsTable: util.ArrayList[CodeNamePair] = {
    this.fieldsTable
  }

  def setFieldsTable(ft: util.ArrayList[CodeNamePair]): Unit = {
    this.fieldsTable = ft
  }

  def getReverseIDs: util.ArrayList[CodeNamePair] = {
    this.reverseIDs
  }

  def setReverseIDs(ids: util.ArrayList[CodeNamePair]): Unit = {
    this.reverseIDs = ids
  }

  def getSigs: util.ArrayList[Sig] = {
    this.sigs
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
    for {
      entry <- associations.entrySet().asScala
      if entry.getKey.equalsIgnoreCase(assName)
    } {
      if (target.equalsIgnoreCase("src")) {
        entry.getValue.setFirst(value)
      } else if (target.equalsIgnoreCase("dst")) {
        entry.getValue.setSecond(value)
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

  def getDataSchemas: util.HashMap[String, util.ArrayList[CodeNamePair]] = {
    this.dataProvider.getTables
  }
}
