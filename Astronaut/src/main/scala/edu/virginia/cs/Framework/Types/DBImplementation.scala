package edu.virginia.cs.Framework.Types

import edu.virginia.cs.Synthesizer.{CodeNamePair, DataProvider, Sig}
import org.w3c.dom.Element

import java.util
import scala.jdk.CollectionConverters._

class DBImplementation(path: String) extends Serializable { //extends FrameworkTypeWrapper {
  private val implPath: String = path
  private var dataProvider: DataProvider = _
  private var reverseTAssociate: util.List[CodeNamePair] = _
  private var foreignKeys: util.List[CodeNamePair] = _
  private var associations: util.Map[String, CodeNamePair] = _
  private var primaryKeys: util.List[CodeNamePair] = _
  private var fields: util.List[CodeNamePair] = _
  private var allFields: util.List[String] = _
  private var fieldsTable: util.List[CodeNamePair] = _
  private var reverseIDs: util.List[CodeNamePair] = _
  private var sigs: util.List[Sig] = _
  private var ids: util.List[String] = _
  private var associationsForCreateSchemas: util.List[String] = _
  private var typeMap: util.Map[String, String] = _

  def getIds: util.List[String] = {
    this.ids
  }

  def setIds(ids: util.List[String]): Unit = {
    this.ids = ids
  }

  def getAssociationsForCreateSchemas: util.List[String] = {
    this.associationsForCreateSchemas
  }

  def setAssociationsForCreateSchemas(asss: util.List[String]): Unit = {
    this.associationsForCreateSchemas = asss
  }

  def getTypeMap: util.Map[String, String] = {
    this.typeMap
  }

  def setTypeMap(typeMap: util.Map[String, String]): Unit = {
    this.typeMap = typeMap
  }

  def getImPath: String = implPath

  // store benchmark path to innerValue
  //makeWrapper(implementationPath)

  def getDataProvider: DataProvider = {
    this.dataProvider
  }

  def setDataProvider(dp: DataProvider): Unit = {
    this.dataProvider = dp
  }

  def getReverseTAssociate: util.List[CodeNamePair] = {
    this.reverseTAssociate
  }

  def setReverseTAssociate(rTAss: util.List[CodeNamePair]): Unit = {
    this.reverseTAssociate = rTAss
  }

  def setForeignKeys(fKeys: util.List[CodeNamePair]): Unit = {
    this.foreignKeys = fKeys
  }

  def getAssociations: util.Map[String, CodeNamePair] = {
    this.associations
  }

  def setAssociations(ass: util.Map[String, CodeNamePair]): Unit = {
    this.associations = ass
  }

  def getPrimaryKeys: util.List[CodeNamePair] = {
    this.primaryKeys
  }

  def setPrimaryKeys(pKeys: util.List[CodeNamePair]): Unit = {
    this.primaryKeys = pKeys
  }

  def getFields: util.List[CodeNamePair] = {
    this.fields
  }

  def setFields(fields: util.List[CodeNamePair]): Unit = {
    this.fields = fields
  }

  def setAllFields(af: util.List[String]): Unit = {
    this.allFields = af
  }

  def getFieldsTable: util.List[CodeNamePair] = {
    this.fieldsTable
  }

  def setFieldsTable(ft: util.List[CodeNamePair]): Unit = {
    this.fieldsTable = ft
  }

  def getReverseIDs: util.List[CodeNamePair] = {
    this.reverseIDs
  }

  def setReverseIDs(ids: util.List[CodeNamePair]): Unit = {
    this.reverseIDs = ids
  }

  def getSigs: util.List[Sig] = {
    this.sigs
  }

  def setSigs(sigs: util.List[Sig]): Unit = {
    this.sigs = sigs
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

  def getSingleAtom(singleTuple: Element, i: Int): String = {
    val atoms = singleTuple.getElementsByTagName("atom")
    val tableCode = atoms.item(i).asInstanceOf[Element]
    var code = tableCode.getAttribute("label")
    val tmp = code.split("/")
    code = tmp(tmp.length - 1)
    code
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

  def getDataSchemas: util.Map[String, util.List[CodeNamePair]] = {
    this.dataProvider.getTables
  }
}
