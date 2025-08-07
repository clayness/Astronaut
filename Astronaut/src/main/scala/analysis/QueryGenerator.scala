package analysis

import edu.virginia.cs.AppConfig
import edu.virginia.cs.Framework.Types.{AbstractQuery, DBImplementation, SpecializedQuery}
import edu.virginia.cs.Synthesizer.{CodeNamePair, Sig}

import java.io.File
import java.util
import scala.jdk.CollectionConverters._

class QueryGenerator {

  def specializeInsertQuery(absq: AbstractQuery, impl: DBImplementation, ins: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): SpecializedQuery = {
    // allInstances here contains all instances in a single object file, which is got by parse the object file
    // some fields may have more than one instance
    // allInstances is a hashmap: HashMap[String, HashMap[String, ArrayList[CodeNamePair[String>>>>
    // HashMap[tableName, HashMap[instanceName, fields_value_pairs]]
    var allInstances: util.Map[String, util.Map[String, util.List[CodeNamePair]]] =
      new util.HashMap[String, util.Map[String, util.List[CodeNamePair]]](1)

    if (absq != null) {
      allInstances = absq.getOodm.parseDocument()
    } else {
      allInstances = ins
    }

    val allInstancesIt = allInstances.entrySet().iterator()

    var field_part: String = ""
    var value_part: String = ""

    val allInsertStmts: util.Map[String, util.Map[Integer, String]] = new util.HashMap[String, util.Map[Integer, String]]()
    /**
     * Prepare output file by implPath
     */
    val implPath = impl.getImPath
    var insertPath = implPath.substring(0, implPath.lastIndexOf(File.separator))
    val implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    insertPath += File.separator + "TestCases"
    if (!new File(insertPath).exists()) {
      new File(insertPath).mkdirs()
    }
    insertPath += File.separator + implFileName + "_insert.sql"

    while (allInstancesIt.hasNext) { // iterate all instances in an object
      val instances = allInstancesIt.next
      val className: String = instances.getKey // get key. tableName
      for (singleInstance <- instances.getValue.asScala) {
        val fieldValuePairs = singleInstance._2
        // key is tableName, and value is fields in the table
        val dbScheme = impl.getDataSchemas
        // which table the element class will be
        val reverseTAss = impl.getReverseTAssociate
        val goToTable = getTableNameByClassName(reverseTAss, className) // null if not found
        if (goToTable != null) {
          /* there is no t_association information for this element
           * which indicates it's an one-to-many association,
           * and the information is in dst table,
           * we don't need to consider since it will be taken in find foreign key value
           */
          val id: String = getPrimaryKeyByTableName(dbScheme, goToTable)
          val id_value: String = getFieldValue(fieldValuePairs, id, impl.getTypeMap)

          field_part = ""
          value_part = ""

          val allAboutSchema: util.List[CodeNamePair] = dbScheme.get(goToTable)
          if (!isClassAssociate(impl, className)) {
            for (pair <- allAboutSchema.asScala if pair.getFirst.equalsIgnoreCase("fields")) {
              /**
               * check pair.getSecond() (fieldName) is in "attr"
               * if fieldName is in attr, call getFieldValue()
               * if fieldName is foreign key, iterate ids and attr
               * to get the primary class (eg, DecisionSpace)
               * if fieldName is DType, set fieldValue as tableName
               */
              val fieldName: String = pair.getSecond
              val fieldInAttr = isFieldInAttr(impl, goToTable, fieldName)
              val fieldIsID = fieldName.equalsIgnoreCase(id)
              val isFKey = isForeignKey(dbScheme, goToTable, fieldName)

              /**
               * Another situation is that,
               * discount is an attribute of PreferredCustomer, however, in schema, it is a field of Customer,
               * when we create insert statement for Customer table, we need to find the value from PreferredCustomer Instance
               * (1) check the tAssociate to see if Customer has the same tAssociate with other tables
               * (2) if so, then check if other tables are children of Customer
               * (3) if so, check if this field is in other tables' attrSet
               * (4) if so, find the value from the instance of that class
               */
              if (!fieldInAttr && !isFKey && !fieldIsID) {
                if (!fieldName.equalsIgnoreCase("DType")) {
                  // find out where this field come from, by iterating all signatures in OM
                  val foreignClass: String = getClassByAttr(impl, fieldName)
                  val fieldValue: String = getForeignValue(allInstances, foreignClass, fieldName, impl.getTypeMap)
                  //                  field_part += "`" + fieldName + "`,"
                  field_part += fieldName + ","
                  value_part += fieldValue + ","
                }
              }

              if (fieldInAttr || fieldIsID) {
                val fieldValue = getFieldValue(fieldValuePairs, fieldName, impl.getTypeMap)
                //                field_part += "`" + fieldName + "`,"
                field_part += fieldName + ","
                value_part += fieldValue + ","
              } else if (fieldName.equalsIgnoreCase("DType")) {
                val fieldValue = "'" + className + "'"
                //                field_part += "`" + fieldName + "`,"
                //                var fieldValue = className
                field_part += fieldName + ","
                value_part += fieldValue + ","
              } else if (isFKey) {
                // the fieldName is a foreign key
                // find the primary class of this foreign key name
                // it is an object signature, not an association
                // NOTICE: there is no need to consider association, since association has its own instances
                val primaryClass = getPrimaryClassById(impl, fieldName)
                // the next step is to get the primary key value of primaryClass
                val pKeyValue = getForeignKeyValue(allInstances, primaryClass, fieldName)
                //                field_part += "`" + fieldName + "`,"
                field_part += fieldName + ","
                value_part += pKeyValue + ","
              }
            }
          } else { // the class is an association
            // find two primary classes by foreign keys
            // find value of the primary key of two primary classes
            for (pair <- allAboutSchema.asScala if pair.getFirst.equalsIgnoreCase("fields")) {
              val keyName = pair.getSecond
              val keyValue = getFieldValue(fieldValuePairs, keyName, impl.getTypeMap)
              //              field_part += "`" + keyName + "`,"
              field_part += keyName + ","
              value_part += keyValue + ","
            }
          }

          field_part = field_part.substring(0, field_part.length() - 1)
          value_part = value_part.substring(0, value_part.length() - 1)
          //          var stmt: String = "INSERT INTO `" + goToTable + "` (" + field_part + ") VALUES (" + value_part + ");"
          val stmt: String = "INSERT INTO " + goToTable + " (" + field_part + ") VALUES (" + value_part + ");"
          //          stmt += "FLUSH TABLES;";
          // add statments
          if (!dataSchemaHasStatement(allInsertStmts, goToTable, Integer.valueOf(id_value))) {
            addInsertStmtIntoDataSchema(allInsertStmts, goToTable, stmt, Integer.valueOf(id_value))
          }
        }
      }
    }
    val sQueries: SpecializedQuery = new SpecializedQuery()
    sQueries.setInsertStmtsInOneObject(allInsertStmts)
    sQueries
  }

  private def getForeignKeyValue(instances: util.Map[String, util.Map[String, util.List[CodeNamePair]]], primaryClass: String, pKey: String): String = {
    val instancesIt = instances.entrySet().iterator()
    while (instancesIt.hasNext) {
      val entry = instancesIt.next()
      val keyName = entry.getKey
      if (keyName.equalsIgnoreCase(primaryClass)) {
        val singleInstanceIt = entry.getValue.entrySet().iterator()
        while (singleInstanceIt.hasNext) {
          for (pair <- singleInstanceIt.next().getValue.asScala) {
            if (pair.getFirst.split("_")(1).equalsIgnoreCase(pKey)) {
              var intValue: Integer = Integer.valueOf(pair.getSecond).intValue()
              val power = scala.math.pow(2, AppConfig.getIntScopeForTestCases - 1)
              intValue = intValue + power.intValue() + 1
              return String.valueOf(intValue)
            }
          }
        }
      }
    }
    null
  }

  private def getPrimaryClassById(impl: DBImplementation, field: String): String = {
    for (pair <- impl.getReverseIDs.asScala) {
      if (pair.getFirst.equalsIgnoreCase(field)) {
        // iterate attrSet
        // check ID is in attr
        for (attr <- impl.getDataProvider.getAttrByTableName(pair.getSecond).asScala) {
          if (attr.equalsIgnoreCase(field)) {
            return pair.getSecond
          }
        }
      }
    }
    null
  }

  private def getForeignValue(instances: util.Map[String, util.Map[String, util.List[CodeNamePair]]], fClass: String, attr: String, types: util.Map[String, String]): String = {
    var value: String = ""
    val instancesIt = instances.entrySet().iterator()
    while (instancesIt.hasNext) {
      val entry = instancesIt.next()
      val keyName = entry.getKey
      if (keyName.equalsIgnoreCase(fClass)) {
        val singleInstanceIt = entry.getValue.entrySet().iterator()
        while (singleInstanceIt.hasNext) {
          for (pair <- singleInstanceIt.next().getValue.asScala) {
            val field = pair.getFirst.split("_")(1)
            if (field.equalsIgnoreCase(attr)) {
              val tmp: String = pair.getSecond
              // get the type of field, then handle the value of it
              val fieldType = types.get(field)
              value = fieldType match {
                case "Int" =>
                  var intValue = Integer.valueOf(tmp).intValue()
                  val power = scala.math.pow(2, AppConfig.getIntScopeForTestCases - 1)
                  intValue = intValue + power.intValue() + 1
                  String.valueOf(intValue)
                case "Real" =>
                  var intValue = Integer.valueOf(tmp).intValue()
                  val power = scala.math.pow(2, AppConfig.getIntScopeForTestCases - 1)
                  intValue = intValue + power.intValue() + 1
                  String.valueOf(intValue)
                //case "Real" =>
                case "Bool" => "0" // Bool in mysql is TinyInt
                case "string" => "'" + tmp + "'"
                case _ => tmp
              }
              return value
            }
          }
        }
      }
    }
    value
  }

  private def getClassByAttr(impl: DBImplementation, attr: String): String = {
    for (sig <- impl.getSigs.asScala) {
      for (sAttr <- sig.getAttrSet.asScala if sig.getCategory == 0) {
        if (sAttr.equalsIgnoreCase(attr)) {
          return sig.getSigName
        }
      }
    }
    null
  }

  private def isForeignKey(scheme: util.Map[String, util.List[CodeNamePair]], table: String, field: String): Boolean = {
    for (pair <- scheme.get(table).asScala) {
      if (pair.getFirst.equalsIgnoreCase("foreignKey")) {
        if (pair.getSecond.equalsIgnoreCase(field)) {
          return true
        }
      }
    }
    false
  }

  private def addInsertStmtIntoDataSchema(allInserts: util.Map[String, util.Map[Integer, String]],
                                          goToTable: String, stmt: String, idValue: Integer): String = {
    val contains: Boolean = allInserts.containsKey(goToTable)
    if (!contains) {
      allInserts.put(goToTable, new util.HashMap[Integer, String])
    }
    allInserts.get(goToTable).put(idValue, stmt)
  }

  private def isFieldInAttr(impl: DBImplementation, mClass: String, attr: String): Boolean = {
    val sigs = impl.getSigs
    for (sig <- sigs.asScala) {
      for (sAttr <- sig.getAttrSet.asScala if sig.getCategory == 0 && sig.getSigName.equalsIgnoreCase(mClass))
        if (sAttr.equalsIgnoreCase(attr)) {
          return true
        }
    }
    false
  }

  private def isClassAssociate(impl: DBImplementation, primaryClass: String): Boolean = {
    impl.getDataProvider.isClassAssociate(primaryClass)
  }

  private def dataSchemaHasStatement(stmts: util.Map[String, util.Map[Integer, String]],
                                     goToTable: String, idValue: Integer): Boolean = {
    if (stmts.containsKey(goToTable)) {
      if (stmts.get(goToTable).containsKey(idValue)) {
        return true
      }
    }
    false
  }

  private def getFieldValue(fieldValues: util.List[CodeNamePair], field: String, types: util.Map[String, String]): String = {
    var value: String = null
    for (pair <- fieldValues.asScala) {
      if (pair.getFirst.split("_")(1).equalsIgnoreCase(field)) {
        val tmp: String = pair.getSecond
        // get the type of field, then handle the value of it
        val fieldType = types.get(field)

        value = fieldType match {
          case "Int" =>
            var intValue = Integer.valueOf(tmp).intValue()
            val power = scala.math.pow(2, AppConfig.getIntScopeForTestCases - 1)
            intValue = intValue + power.intValue() + 1
            String.valueOf(intValue)
          case "Real" =>
            var intValue = Integer.valueOf(tmp).intValue()
            val power = scala.math.pow(2, AppConfig.getIntScopeForTestCases - 1)
            intValue = intValue + power.intValue() + 1
            String.valueOf(intValue)
          //case "Real" =>
          case "Bool" => "0"
          case "string" => "'" + tmp + "'"
          case _ => tmp
        }
        return value
      }
    }
    value
  }

  private def getPrimaryKeyByTableName(dbScheme: util.Map[String, util.List[CodeNamePair]], tableName: String): String = {
    val table: util.List[CodeNamePair] = dbScheme.get(tableName)
    //    var pair: CodeNamePair = null
    for (pair <- table.asScala) {
      if (pair.getFirst.equalsIgnoreCase("primaryKey")) {
        return pair.getSecond
      }
    }
    null
  }

  // looks up reverse t_associate data structure to find a target table for each object element, e.g. a class instance or an association
  private def getTableNameByClassName(reverseTAss: util.List[CodeNamePair], className: String): String = {
    for (elem <- reverseTAss.asScala) {
      if (elem.getFirst.equalsIgnoreCase(className)) {
        return elem.getSecond
      }
    }
    null
  }

  def specializeSelectQuery(absq: AbstractQuery, impl: DBImplementation, ins: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): SpecializedQuery = {
    var selectPart = ""
    var fromPart = ""
    var wherePart = ""
    val allSelectStmts: util.Map[String, util.Map[Integer, String]] = new util.HashMap[String, util.Map[Integer, String]](1)

    var instance: util.Map[String, util.Map[String, util.List[CodeNamePair]]] = new util.HashMap[String, util.Map[String, util.List[CodeNamePair]]](1)

    if (absq != null) {
      instance = absq.getOodm.parseDocument()
    } else {
      instance = ins
    }

    for (instanceEntry <- instance.asScala) {
      var element = instanceEntry._1
      val instance = instanceEntry._2
      val isAss = isAssociation(impl.getSigs, element)
      if (!isAss) {
        for (singleInstance <- instance.asScala) {
          val fieldValuePairs = singleInstance._2

          selectPart = "SELECT "
          fromPart = " FROM "
          wherePart = " WHERE "

          val dbScheme = impl.getDataSchemas
          var goToTable = getTableNameByClassName(impl.getReverseTAssociate, element)

          val id: String = getPrimaryKeyByTableName(dbScheme, goToTable)
          val id_value: Integer = getFieldValue(fieldValuePairs, id, impl.getTypeMap).toInt

          val parent = getParent(impl.getSigs)
          if (parent == null) { // element is a root class
            val allAboutOMClass: util.List[CodeNamePair] = dbScheme.get(goToTable)
            //            fromPart += "`" + element + "`"
            fromPart += element
            for (pair <- allAboutOMClass.asScala if pair.getFirst.equalsIgnoreCase("fields")) {
              val fieldName = pair.getSecond
              //              selectPart += "`" + element + "`.`" + fieldName + "`,"
              selectPart += element + "." + fieldName + ","
              if (isPrimaryKeys(impl.getPrimaryKeys, element, fieldName)) {
                val value = getFieldValue(fieldValuePairs, fieldName, impl.getTypeMap)
                //                wherePart += "`" + element + "`.`" + fieldName + "`=" + value + " AND "
                wherePart += element + "." + fieldName + "=" + value + " AND "
              }
            }
          } else if (!goToTable.equalsIgnoreCase(element)) { // class C is mapped to the same table as its super class

          } else if (goToTable.equalsIgnoreCase(element)) { // class C is mapped to its own table
            //            fromPart += "`" + goToTable + "`";
            fromPart += goToTable
            val allAboutOMClass: util.List[CodeNamePair] = dbScheme.get(goToTable)
            for (pair <- allAboutOMClass.asScala if pair.getFirst.equalsIgnoreCase("fields")) {
              val fieldName = pair.getSecond
              //              selectPart += "`" + element + "`.`" + fieldName + "`,";
              selectPart += element + "." + fieldName + ","
              if (isPrimaryKeys(impl.getPrimaryKeys, element, fieldName)) {
                val value = getFieldValue(fieldValuePairs, fieldName, impl.getTypeMap)
                //                wherePart += "`" + element + "`.`" + fieldName + "`=" + value + " AND ";
                wherePart += element + "." + fieldName + "=" + value + " AND "
              }
            }
          }
          selectPart = selectPart.substring(0, selectPart.length() - 1)
          wherePart = wherePart.substring(0, wherePart.length() - 5)
          val stmt = selectPart + fromPart + wherePart + ";"
          if (!stmt.substring(0, 11).equalsIgnoreCase("select from")) {
            //            stmt += "RESET QUERY CACHE;";
            if (!dataSchemaHasStatement(allSelectStmts, goToTable, id_value)) {
              addSelectStmtIntoDataSchema(allSelectStmts, goToTable, id_value, stmt)
            }
          }
        }
      }
    }
    val sq = new SpecializedQuery()
    sq.setSelectStmtsInOneObject(allSelectStmts)
    sq
  }

  private def isAssociation(sigs: util.List[Sig], element: String): Boolean = {
    for (sig <- sigs.asScala) {
      if (sig.getCategory == 1 && sig.getSigName.equalsIgnoreCase(element)) {
        return true
      }
    }
    false
  }

  def getParent(sigs: util.List[Sig]): String = {
    for (sig <- sigs.asScala) {
      if (sig.getCategory == 0) {
        if (sig.isHasParent) {
          return sig.getParent
        }
      }
    }
    null
  }

  private def isPrimaryKeys(pKeys: util.List[CodeNamePair], table: String, field: String): Boolean = {
    pKeys.asScala.exists(pair => pair.getFirst.equalsIgnoreCase(table) && pair.getSecond.equalsIgnoreCase(field))
  }

  private def addSelectStmtIntoDataSchema(allStmts: util.Map[String, util.Map[Integer, String]],
                                          tableName: String, idValue: Integer, stmt: String): String = {
    if (!allStmts.containsKey(tableName)) {
      allStmts.put(tableName, new util.HashMap[Integer, String]())
    }
    allStmts.get(tableName).put(idValue, stmt)
  }
}
