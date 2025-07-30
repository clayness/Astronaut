package edu.virginia.cs

import edu.virginia.cs.Framework.Types.AbstractQuery.Action
import edu.virginia.cs.Framework.Types._
import edu.virginia.cs.Framework._
import edu.virginia.cs.Synthesizer._
import edu.virginia.cs.Uniq.DeleteUniq
import org.apache.spark.{SparkConf, SparkContext}

import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.Path
import java.text.{NumberFormat, ParsePosition}
import java.util
import java.util.{Random, UUID}
import scala.jdk.CollectionConverters._
import scala.util.Using

class DBTrademaker extends AstronautFramework {

  // analyze and tradespace are already defined in Tradespace specification
  // we can call "tradespace" function to synthesize implementation and benchmark
  private val myTradespace: Tradespace = Build_Tradespace(synthesizeImplAndFuncFromSpec, myRunBenchmark, myMapReduce)
  var isDebugOn: Boolean = AppConfig.getDebug
  var startTime: Long = 1
  var endTime: Long = 1
  private var timeInterval: Long = 1

  def run(): Unit = {
    // iterate the list of specs from the configuration
    val specs = AppConfig.getSpecs
    for (spec <- specs) {
      // get the name of the specification file
      val leaf = Path.of(spec).getFileName.toString.replaceFirst("\\.[^.]+$", "")
      // synthesize the tradespace for the spec
      val mySpec: DBSpecification = new DBSpecification(spec)
      val evaluatedResults = tradespaceFunction(mySpec)

      // if there are any results, create the output files
      if (evaluatedResults.nonEmpty) {
        // get solution file name, which is like: customerOrderObjectModel_Sol_2.sql
        val outPath = Path.of(evaluatedResults.head._1.getImPath).resolveSibling(leaf + ".txt")

        // iterate the results and print each result to the output file
        Using(new PrintWriter(outPath.toFile)) { pw =>
          for ((impl, mr) <- evaluatedResults) {
            val sol = impl.getImPath
            val solName = sol.substring(sol.lastIndexOf(File.separator) + 1, sol.lastIndexOf("."))
            pw.printf("%s:%d:%d:%d%n", solName,
              mr.getTmr.getInsertTime,
              mr.getTmr.getSelectTime,
              mr.getSmr.getDbSpace)
          }
        }
      }
    }
  }

  def getIDBySigName(sigs: util.List[Sig], sigName: String): String = {
    for (s <- sigs.asScala) {
      if (s.getSigName.equalsIgnoreCase(sigName)) {
        return s.getId
      }
    }
    ""
  }

  def getAssByKey(scheme: util.Map[String, util.List[CodeNamePair]],
                  pTable: String, fTable: String): util.Map[String, CodeNamePair] = {
    val ass_map: util.Map[String, CodeNamePair] = new util.HashMap[String, CodeNamePair]()
    var src: String = ""
    var dst: String = ""
    var ass: String = ""

    val schemeIt = scheme.asScala.iterator
    while (schemeIt.hasNext) {
      val table = schemeIt.next
      val fields = table._2
      for (pair <- fields.asScala) {
        if (pair.getFirst.equalsIgnoreCase("src")) {
          if (pair.getSecond.equalsIgnoreCase(pTable)) {
            src = pTable
          }
          if (pair.getSecond.equalsIgnoreCase(fTable)) {
            src = fTable
          }
        }
        if (pair.getFirst.equalsIgnoreCase("dst")) {
          if (pair.getSecond.equalsIgnoreCase(pTable)) {
            dst = pTable
          }
          if (pair.getSecond.equalsIgnoreCase(fTable)) {
            dst = fTable
          }
        }
      }
      if (src.nonEmpty && dst.nonEmpty) {
        ass = table._1
        val pair: CodeNamePair = new CodeNamePair(src, dst)
        ass_map.put(ass, pair)
        return ass_map
      }
    }
    null
  }

  // get table name by the primary key
  // we need to filter out the association table by check if the primary key is foreign key at the same time
  def getTablesByPrimaryKey(pKeys: util.List[CodeNamePair], primaryKey: String): util.List[String] = {
    val tables: util.List[String] = new util.ArrayList[String]()
    for (pair <- pKeys.asScala) {
      if (pair.getSecond.equalsIgnoreCase(primaryKey)) {
        tables.add(pair.getFirst)
      }
    }
    tables
  }

  def isNumeric(str: String): Boolean = {
    val formatter: NumberFormat = NumberFormat.getInstance()
    val pos: ParsePosition = new ParsePosition(0)
    formatter.parse(str, pos)
    str.length() == pos.getIndex
  }

  private def synthesizeImplAndFuncFromSpec(spec: SpecificationType)
  : List[(ImplementationType, MeasurementFunctionSetType)] = {
    val fSpec: FormalSpecificationType = createFormalSpec(spec)
    val fImpl: List[FormalImplementationType] = createFormalImpls(fSpec)
    val impls = fImpl.map(myIFunction)

    if (AppConfig.getIsRandom == 0) {
      val fAbsMF: FormalAbstractMeasurementFunctionSet = myLFunction(fSpec)
      val fConMF: List[FormalConcreteMeasurementFunctionSet] = myTFunction(fAbsMF)(impls)
      val mfs = fConMF.map(myBFunction)
      val zipped = impls.zip(mfs)
      return zipped
    } else if (AppConfig.getIsRandom == 1) {
      // get concrete measurement function by random generator
      val mfs = genRandomConcreteMF(impls)
      // iterate to create list of pairs, call "combine" will result in StackOverFlowError
      val zipped = impls.zip(mfs)
      return zipped
    }
    null
  }

  private def genRandomConcreteMF(impls: List[ImplementationType]): List[MeasurementFunctionSetType] = {
    /**
     * convert List[ImplementationType] to ArrayList[DBImplementation]
     * iterate all implementations and create DBMeasurementFunction
     */
    var mfSets: List[MeasurementFunctionSetType] = Nil

    val dbImpls = impls.map(e => new DBImplementation(e.getImPath))
    val range = AppConfig.getRandomRange
    val allInstances = generateRandomInstances(dbImpls.head.getSigs, dbImpls.head.getTypeMap, 1, range)

    for (singleImpl <- dbImpls) {
      val ctmf: DBConcreteTimeMeasurementFunction = new DBConcreteTimeMeasurementFunction()
      ctmf.setInstances(allInstances)
      ctmf.setImpl(singleImpl)

      val csmf: DBConcreteSpaceMeasurementFunction = new DBConcreteSpaceMeasurementFunction()
      csmf.setInstances(allInstances)
      csmf.setImpl(singleImpl)

      val mfs: DBConcreteMeasurementFunctionSet = new DBConcreteMeasurementFunctionSet(ctmf, csmf)
      mfSets = mfs :: mfSets
    }
    mfSets
  }

  //noinspection SameParameterValue
  private def generateRandomInstances(sigs: util.List[Sig], types: util.Map[String, String], low: Integer, high: Integer): util.Map[String, util.Map[String, util.List[CodeNamePair]]] = {
    val instances = new util.HashMap[String, util.Map[String, util.List[CodeNamePair]]](5)

    val lowValue = low.intValue()
    val highValue = high.intValue()
    val range = highValue - lowValue
    for (sig <- sigs.asScala) {
      for (i <- lowValue to highValue) {
        val sigName = sig.getSigName
        val instanceName = sigName + i
        if (!instances.containsKey(sigName)) {
          instances.put(sigName, new util.HashMap[String, util.List[CodeNamePair]](3))
        }
        if (!instances.get(sigName).containsKey(instanceName)) {
          instances.get(sigName).put(instanceName, new util.ArrayList[CodeNamePair](3))
        }

        if (sig.getCategory == 0) { // 0 is class
          val id: String = sig.getId
          var fieldValue: String = String.valueOf(i)
          instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + id, fieldValue))
          for (fieldName <- sig.getAttrSet.asScala) {
            if (!fieldName.equalsIgnoreCase(id)) {
              val fieldType: String = types.get(fieldName)
              if (fieldType.contains("Int")) {
                val rand: Random = new Random(System.currentTimeMillis())
                fieldValue = String.valueOf(lowValue + rand.nextInt(range))
              } else if (fieldType.equalsIgnoreCase("string")) {
                fieldValue = fieldName + UUID.randomUUID().toString.substring(0, 6)
              } else if (fieldType.equalsIgnoreCase("Bool")) {
                // assign it as true
                fieldValue = "true"
              }
              instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + fieldName, fieldValue))
            }
          }
        } else if (sig.getCategory == 1) { // 1 is association
          val srcIDName: String = getIDBySigName(sigs, sig.getSrc)

          val srcIDValue = i
          val dstIDName = getIDBySigName(sigs, sig.getDst)

          val dstIDValue = i
          instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + srcIDName, String.valueOf(srcIDValue)))
          instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + dstIDName, String.valueOf(dstIDValue)))
        }
      }
    }
    instances
  }

  // here we get the tradespace function and analyze function
  private def tradespaceFunction = tradespace(myTradespace) // fun: (SpecificationType => List[Prod[ImplementationType, BenchmarkResultType]])

  /**
   * Use Spark to evaluate solutions
   *
   * @return list of implementation and measurement result
   */
  private def myMapReduce(list: List[(ImplementationType, MeasurementFunctionSetType)]): List[(ImplementationType, MeasurementResultSetType)] = {
    /**
     * map run benchmark function to the list of measurement functions
     * 1. create Spark context
     * 2. create RDD based on the list
     * 3. call Spark's map to execute
     */
    val conf = new SparkConf().setAppName("Astronaut")
      .set("spark.akka.frameSize", "200")
      .set("spark.default.parallelism", "16")
      .set("spark.storage.blockManagerSlaveTimeoutMs", "600000")
      .set("spark.worker.timeout", "600000")
      .set("spark.akka.timeout", "600000")
      .set("spark.rpc.askTimeout", "600000")
      .set("spark.rpc.retry.wait", "600000")
      .set("spark.rpc.message.maxSize", "300")
      .set("spark.storage.memoryFraction", "0.9")

    val sc = new SparkContext(conf)
    val rdd = sc.parallelize(list)
    val evaluationResult = rdd.map(e => {
      val result = myRunBenchmark(e._1, e._2)
      result
    })

    val collectedResult = evaluationResult.collect()
    endTime = System.currentTimeMillis
    timeInterval = endTime - startTime

    sc.stop()
    collectedResult.toList
  }

  private def myRunBenchmark(impl: ImplementationType, mfs: MeasurementFunctionSetType): (ImplementationType, MeasurementResultSetType) = {
    mfs.setImpl(impl)

    // If benchmark is random test loads, need to create concrete load first and then run them
    if (AppConfig.getIsRandom == 1) {
      // call concrete loads creator here
      val timeLoads: util.List[ConcreteLoad] = new util.ArrayList[ConcreteLoad](2)
      val spaceLoads: util.List[ConcreteLoad] = new util.ArrayList[ConcreteLoad](1)
      val insertLoad: ConcreteLoad = generateRandomInsertStatements(impl, mfs.getCtmf.getInstances)
      val selectLoad: ConcreteLoad = generateRandomSelectStatements(impl, mfs.getCtmf.getInstances)
      timeLoads.add(insertLoad)
      timeLoads.add(selectLoad)

      spaceLoads.add(insertLoad)
      mfs.getCtmf.setLoads(timeLoads)
      mfs.getCsmf.setLoads(spaceLoads)
    }

    // wait for a while to collect since test suite transfer might require sometime


    /**
     * iterate all measurement function set to get insert and select script file
     * check if there are available nodes
     * if Yes, start a Worker to continue work
     * if No, sleep 3 seconds and then try to get an available nodes
     */
    //    if (AppConfig.getIsRandom() == 1) {
    //      var mr: DBMeasurementResult = Evaluator.Evaluator.evaluate(impl, mfs)
    //      mr.setImpl(impl)
    //      myPair = Pair[ImplementationType, MeasurementResultSetType](impl, mr)
    //    } else {
    val tmf = mfs.getCtmf
    tmf.setImpl(impl)
    val smf = mfs.getCsmf
    smf.setImpl(impl)
    if (isDebugOn) {
      println("=======================")
      println("TimeMeasurementFunction")
      println("=======================")
    }
    val tmr = tmf.run()
    println("Insert time: " + tmr.getInsertTime + "s. Select time: " + tmr.getSelectTime + "s.")
    if (isDebugOn) {
      println("=======================")
      println("SpaceMeasurementFunction")
      println("=======================")
    }
    val smr = smf.run()
    if (isDebugOn) {
      println("DB space: " + smr.getDbSpace + "kb")
    }

    val dbmr = new DBMeasurementResult(tmr, smr)
    dbmr.setImpl(impl)
    //    }

    // delete insert and select files
    val loads = mfs.getCtmf.getLoads.asScala
    for (load <- loads) {
      val inPath = load.getInsertPath
      val slPath = load.getSelectPath

      val inFile = new File(inPath)
      if (inFile.exists()) {
        //        Chong:
        inFile.delete()
      }
      val slFile = new File(slPath)
      if (slFile.exists()) {
        //        Chong:
        slFile.delete()
      }
    }

    if (isDebugOn) {
      println("finish implementation: " + impl.getImPath + ":" + dbmr.getTmr.getInsertTime + "," + dbmr.getTmr.getSelectTime + "," + dbmr.getSmr.getDbSpace)
    }

    // we can write the results into hadoop file system
    // /trademaker/modelName/solutionName

    (new DBImplementation(impl.getImPath), dbmr)
  }

  // get the random instances, and return insertFilePath
  private def generateRandomInsertStatements(impl: DBImplementation, instances: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): ConcreteLoad = {
    val insertSpecializedQuery: SpecializedQuery = specializeInsertQuery(null, impl, instances)
    val cq = new ConcreteQuery()
    cq.setAction(Action.INSERT)
    cq.setSq(insertSpecializedQuery)
    val cqs = new util.ArrayList[ConcreteQuery](1)
    cqs.add(cq)
    /**
     * print out insert scripts
     */
    val insCL = new ConcreteLoad()
    insCL.setQuerySet(cqs)

    val implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    val implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs()
    }
    val insertPath = pathBase + File.separator + implFileName + "_insert.sql"
    insCL.setInsertPath(insertPath)
    insCL.setSelectPath("")
    val insertFile: File = new File(insertPath)
    if (!insertFile.exists()) {
      insertFile.createNewFile()
    }

    val insertPw: PrintWriter = new PrintWriter(new FileWriter(insertPath, true))
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      insertPw.println("USE " + implFileName + ";")
    } else if (AppConfig.getTestDB.equalsIgnoreCase("postgres")) {
      insertPw.println("BEGIN;")
    }
    val allInsertStmts = new util.ArrayList[util.Map[String, util.Map[Integer, String]]]().asScala
    for (elem <- insCL.getQuerySet.asScala) {
      val sq = elem.getSq
      val sqInOneObject = sq.getInsertStmtsInOneObject
      allInsertStmts.asJava.add(sqInOneObject)
    }

    val printOrder = PrintOrder.getOutPutOrders(pathBase).asScala
    for (s <- printOrder) {
      for (insertS <- allInsertStmts) {
        val mapIt = insertS.asScala.iterator
        while (mapIt.hasNext) {
          val table = mapIt.next // (String, HashMap[Integer, String]) = (tableName, HashMap[ID, Statements])
          if (table._1.equalsIgnoreCase(s)) { // check table name
            val stmt = table._2
            val stmtIt = stmt.asScala.iterator
            while (stmtIt.hasNext) {
              val idStmt = stmtIt.next
              val stmtStr: String = String.valueOf(idStmt._2.toCharArray)
              insertPw.println(stmtStr)
            }
            insertPw.flush()
          }
        }
      }
    }

    insCL.setInsertPath(insertPath)
    if (AppConfig.getTestDB.equalsIgnoreCase("postgres")) {
      insertPw.println("COMMIT;")
    }
    insertPw.flush()
    insertPw.close()

    insCL.getQuerySet.clear()
    insertSpecializedQuery.getInsertStmtsInOneObject.clear()
    insertSpecializedQuery.getSelectStmtsInOneObject.clear()

    insCL
  }

  private def specializeInsertQuery(absq: AbstractQuery, impl: DBImplementation, ins: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): SpecializedQuery = {
    // allInstances here contains all instances in a single object file, which is got by parse the object file
    // some fields may have more than one instance
    // allInstances is a hashmap: HashMap[String, HashMap[String, ArrayList[CodeNamePair[String>>>>
    // HashMap[tableName, HashMap[instanceName, fields_value_pairs]]
    var allInstances: util.Map[String, util.Map[String, util.List[CodeNamePair]]] = new util.HashMap[String, util.Map[String, util.List[CodeNamePair]]](1)

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
      val instancesIt = instances.getValue.asScala.iterator
      while (instancesIt.hasNext) { // iterate all instances for one class
        val singleInstance = instancesIt.next
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
          if (!dataSchemaHasInsertStatement(allInsertStmts, goToTable, Integer.valueOf(id_value))) {
            addInsertStmtIntoDataSchema(allInsertStmts, goToTable, stmt, Integer.valueOf(id_value))
          }
        }
      }
    }
    val sQueries: SpecializedQuery = new SpecializedQuery()
    sQueries.setInsertStmtsInOneObject(allInsertStmts)
    sQueries
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

  /*
   *  purpose is to convert a given abstract measurement function (set of insert or select abstract queries) into a concrete measurement
   *  function, specialized to a particular implementation.
  */

  private def isClassAssociate(impl: DBImplementation, primaryClass: String): Boolean = {
    impl.getDataProvider.isClassAssociate(primaryClass)
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

  def addInsertStmtIntoDataSchema(allInserts: util.Map[String, util.Map[Integer, String]],
                                  goToTable: String, stmt: String, idValue: Integer): String = {
    val contains: Boolean = allInserts.containsKey(goToTable)
    if (!contains) {
      allInserts.put(goToTable, new util.HashMap[Integer, String])
    }
    allInserts.get(goToTable).put(idValue, stmt)
  }

  def dataSchemaHasInsertStatement(allInserts: util.Map[String, util.Map[Integer, String]],
                                   goToTable: String, idValue: Integer): Boolean = {
    if (allInserts.containsKey(goToTable)) {
      if (allInserts.get(goToTable).containsKey(idValue)) {
        return true
      }
    }
    false
  }

  def getForeignKeyValue(instances: util.Map[String, util.Map[String, util.List[CodeNamePair]]], primaryClass: String, pKey: String): String = {
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

  def isForeignKey(scheme: util.Map[String, util.List[CodeNamePair]], table: String, field: String): Boolean = {
    for (pair <- scheme.get(table).asScala) {
      if (pair.getFirst.equalsIgnoreCase("foreignKey")) {
        if (pair.getSecond.equalsIgnoreCase(field)) {
          return true
        }
      }
    }
    false
  }

  def getFieldValue(fieldValues: util.List[CodeNamePair], field: String, types: util.Map[String, String]): String = {
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

  def getPrimaryKeyByTableName(dbScheme: util.Map[String, util.List[CodeNamePair]], tableName: String): String = {
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

  // get the random instances, and return insertFilePath
  private def generateRandomSelectStatements(impl: DBImplementation, instances: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): ConcreteLoad = {
    val selectSpecializedQuery: SpecializedQuery = specializeSelectQuery(null, impl, instances)

    val cq = new ConcreteQuery()
    cq.setAction(Action.SELECT)
    cq.setSq(selectSpecializedQuery)
    val cqs = new util.ArrayList[ConcreteQuery]()
    cqs.add(cq)
    val selCL = new ConcreteLoad()
    selCL.setQuerySet(cqs)

    // convert select statements

    val implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    val implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs()
    }
    val printOrder = PrintOrder.getOutPutOrders(pathBase).asScala

    val selectPath = pathBase + File.separator + implFileName + "_select.sql"
    //    val compressedSelectPath = pathBase + File.separator + implFileName + "_select.sql.tar.gz"
    //    var tmpSelectPath = pathBase + File.separator + implFileName + "_select_tmp.sql"
    selCL.setSelectPath(selectPath)
    selCL.setInsertPath("")
    val selectFile: File = new File(selectPath)
    //    var tmpSelectFile: File = new File(tmpSelectPath)
    if (!selectFile.exists()) {
      selectFile.createNewFile()
    }

    val selectPw: PrintWriter = new PrintWriter(new FileWriter(selectPath, true))
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      selectPw.println("USE " + implFileName + ";")
    }
    val allSelectStmts = new util.ArrayList[util.Map[String, util.Map[Integer, String]]]().asScala

    for (elem <- selCL.getQuerySet.asScala) {
      val sq = elem.getSq
      val sqInOneObject = sq.getSelectStmtsInOneObject
      allSelectStmts.asJava.add(sqInOneObject)
    }

    for (s: String <- printOrder) {
      for (selectS <- allSelectStmts) {
        val mapIt = selectS.asScala.iterator
        while (mapIt.hasNext) {
          val elem = mapIt.next // (String, HashMap[Integer, String]) = (tableName, HashMap[ID, Statements])
          if (elem._1.equalsIgnoreCase(s)) {
            val tmp = elem._2
            val tmpIt = tmp.asScala.iterator
            while (tmpIt.hasNext) {
              val stmt = tmpIt.next
              val stmtStr = String.valueOf(stmt._2.toCharArray)
              selectPw.println(stmtStr)
            }
            selectPw.flush()
          }
        }
      }
    }

    selCL.setSelectPath(selectPath)
    selectPw.flush()
    selectPw.close()

    /**
     * // compress test cases and delete sql file
     * Process(Seq("tar", "czf", compressedSelectPath, "-C", pathBase, implFileName + "_select.sql")).!
     * Process(Seq("rm", selectPath)).!
     */
    selCL.getQuerySet.clear()
    selectSpecializedQuery.getInsertStmtsInOneObject.clear()
    selectSpecializedQuery.getSelectStmtsInOneObject.clear()

    selCL
  }

  private def specializeSelectQuery(absq: AbstractQuery, impl: DBImplementation, ins: util.Map[String, util.Map[String, util.List[CodeNamePair]]]): SpecializedQuery = {
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

    val instanceIt = instance.asScala.iterator
    while (instanceIt.hasNext) {
      val instanceEntry = instanceIt.next
      var element = instanceEntry._1
      val instance = instanceEntry._2
      val isAss = isAssociation(impl.getSigs, element)
      if (!isAss) {
        val instanceIt = instance.asScala.iterator
        while (instanceIt.hasNext) {
          val singleInstance = instanceIt.next
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
            if (!dataSchemaHasSelectStatement(allSelectStmts, goToTable, id_value)) {
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

  def isAssociation(sigs: util.List[Sig], element: String): Boolean = {
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

  def isPrimaryKeys(pKeys: util.List[CodeNamePair], table: String, field: String): Boolean = {
    for (s <- pKeys.asScala) {
      if (s.getFirst.equalsIgnoreCase(table) && s.getSecond.equalsIgnoreCase(field)) {
        return true
      }
    }
    false
  }

  def dataSchemaHasSelectStatement(stmts: util.Map[String, util.Map[Integer, String]], tableName: String, idValue: Integer): Boolean = {
    if (stmts.containsKey(tableName)) {
      if (stmts.get(tableName).containsKey(idValue)) {
        return true
      }
    }
    false
  }

  def addSelectStmtIntoDataSchema(allStmts: util.Map[String, util.Map[Integer, String]],
                                  tableName: String, idValue: Integer, stmt: String): String = {
    if (!allStmts.containsKey(tableName)) {
      allStmts.put(tableName, new util.HashMap[Integer, String]())
    }
    allStmts.get(tableName).put(idValue, stmt)
  }

  private def createFormalImpls(fSpec: FormalSpecificationType): List[FormalImplementationType] = {
    var solFolder: String = AppConfig.getSolutionFolder
    if (solFolder == null || solFolder.trim.isEmpty) {
      startTime = System.currentTimeMillis()
      /*
       * get specification file path
       * calculate solution folder based on specification file path
       * call smartbridge() function to synthesize formal implementations
       * scan solution folder to get implementations
       */
      val specPath: String = fSpec.getSpec
      solFolder = specPath.substring(0, specPath.lastIndexOf(File.separator))
      val alloyOMName = specPath.substring(specPath.lastIndexOf(File.separator) + 1, specPath.lastIndexOf("."))
      solFolder = solFolder + File.separator + alloyOMName + File.separator + "ImplSolution"
      recursiveDelete(new File(solFolder))
      if (!new File(solFolder).exists()) {
        val fileFP = new File(solFolder)
        val rtn = fileFP.mkdirs()
        if (rtn) {
          println("Solution folder created!:::" + solFolder)
        } else {
          if (isDebugOn) {
            println("Create solution folder failed!")
          }
        }
      }

      // get mapping run file
      val mappingRun: String = FileOperation.getMappingRun(specPath)
      // call smartBridge
      new SmartBridge(solFolder, mappingRun, AppConfig.getMaxSolForImpl.intValue())

      // delete duplicate solutions
      DeleteUniq.del(solFolder)

      // log the time taken to synthesize all the results
      val synthTime = System.currentTimeMillis() - startTime
      println(s"[ICSE2022] synthesis complete: time=$synthTime, spec=$specPath")
    } else {
      println(s"[ICSE2020] reading results from folder: $solFolder")
    }

    getDatabaseImplsForFolder(fSpec, solFolder)
  }

  private def getDatabaseImplsForFolder(fSpec: FormalSpecificationType, solFolder: String): List[FormalImplementationType] = {
    // scan solution folder, and get all the solutions
    val solFiles: Array[File] = new java.io.File(solFolder).listFiles.filter(_.getName.endsWith(".xml"))
    var implList: List[FormalImplementationType] = Nil

    for (file <- solFiles) {
      val dbImpl: DBFormalImplementation = new DBFormalImplementation()
      dbImpl.setImp(file.getAbsolutePath)
      dbImpl.setSigs(fSpec.getSigs)
      dbImpl.setAssociationsForCreateSchemas(fSpec.getAssociations)
      dbImpl.setTypeMap(fSpec.getTypeMap)
      dbImpl.setIds(fSpec.getIds)
      implList = dbImpl :: implList
    }
    implList
  }

  private def myLFunction(fSpec: FormalSpecificationType): FormalAbstractMeasurementFunctionSet = {
    // generate two abstract load objects:  insert and select
    // create two measurement functions, one for time, one for space
    // wrap them in a FormalAbstractMeasurementFunctionSet
    val absLoads: FormalAbstractLoadSet = generateFormalAbstractLoadSet(fSpec)
    val absTimeMeasurementFunction = new DBFormalAbstractTimeMeasurementFunction(absLoads.getInsLoad, absLoads.getSelLoad)
    val absSpaceMeasurementFunction = new DBFormalAbstractSpaceMeasurementFunction(absLoads.getInsLoad)
    new DBFormalAbstractMeasurementFunctionSet(absTimeMeasurementFunction, absSpaceMeasurementFunction)
  }

  private def recursiveDelete(f: File): Boolean = {
    if (f.isDirectory) {
      for (c <- f.listFiles())
        recursiveDelete(c)
    }
    if (!f.delete()) {
      //      throw new FileNotFoundException("Failed to delete file: " + f)
    }
    true
  }

  private def generateFormalAbstractLoadSet(fSpec: FormalSpecificationType): FormalAbstractLoadSet = {

    // initialize two empty abstract loads objects, to contain insert and select queries
    // for each object solution to fSpec
    //     - generate abstract insert and select queries,
    //			and add them to the corresponding abstract load objects
    // package up the two abstract load objects in a FormalAbstractLoadSet object and return the result

    // initialize two empty abstract loads objects, to contain insert and select queries
    val insAbsLoad = new AbstractLoad()
    val selAbsLoad = new AbstractLoad()

    // use Alloy analyzer
    // for each object solution to fSpec
    val objSpec = genObjSpec(fSpec)

    // construct path to solution folder
    val specPath = objSpec.getSpecPath
    val lenOfExtension = "_dm.als".length()
    var objectSolFolder = specPath.substring(0, specPath.length() - lenOfExtension)

    objectSolFolder = objectSolFolder + File.separator + "TestSolutions"
    recursiveDelete(new File(objectSolFolder))
    new File(objectSolFolder).mkdirs()

    // call objects generator
    val loadSynthesizer = new LoadSynthesizer()
    loadSynthesizer.genObjsHelper(specPath, objectSolFolder, fSpec.getIds) // parse ID for negation

    /*
     * get solutions to alloy spec (stored as XML files)
     *
     * for each such solution ("object")
     *    * generate two abstract queries
     *    * add queries to relevant abstract load objects
     */

    // iterate all objects
    val objectFiles: Array[File] = new java.io.File(objectSolFolder).listFiles.filter(_.getName.endsWith(".xml"))

    val insQuerySet: util.List[AbstractQuery] = new util.ArrayList()
    val selQuerySet: util.List[AbstractQuery] = new util.ArrayList()

    for (file <- objectFiles) {
      val singleObject = new ObjectOfDM(file.getAbsolutePath)

      val insQuery: AbstractQuery = new AbstractQuery(AbstractQuery.Action.INSERT, singleObject)
      val selQuery: AbstractQuery = new AbstractQuery(AbstractQuery.Action.SELECT, singleObject)

      insQuerySet.add(insQuery)
      selQuerySet.add(selQuery)
    }
    insAbsLoad.setQuerySet(insQuerySet)
    selAbsLoad.setQuerySet(selQuerySet)

    new FormalAbstractLoadSet(insAbsLoad, selAbsLoad)
  }

  private def getConcreteMeasurementFunctionSet(absMF: DBFormalAbstractMeasurementFunctionSet, impl: DBImplementation): DBFormalConcreteMeasurementFunctionSet = {
    val concMFSet: DBFormalConcreteMeasurementFunctionSet = new DBFormalConcreteMeasurementFunctionSet()

    // for time
    val tmf: DBFormalAbstractMeasurementFunction = absMF.getTmf
    val tmfALoads: util.List[AbstractLoad] = tmf.getLoads
    val insAL: AbstractLoad = tmfALoads.get(0)
    val selAL: AbstractLoad = tmfALoads.get(1)
    val insCL = convert(insAL, impl)
    /**
     * print out insert scripts
     */
    val implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    val implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs()
    }
    val insertPath = pathBase + File.separator + implFileName + "_insert.sql"
    insCL.setInsertPath(insertPath)
    val insertFile: File = new File(insertPath)
    if (insertFile.exists()) {
      insertFile.delete()
      insertFile.createNewFile()
    } else {
      insertFile.createNewFile()
    }
    val insertPw: PrintWriter = new PrintWriter(insertFile)
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      insertPw.println("USE " + implFileName + ";")
    }
    val allInsertStmts = new util.ArrayList[util.Map[String, util.Map[Integer, String]]]()
    val printOrder = PrintOrder.getOutPutOrders(pathBase)
    for (elem <- insCL.getQuerySet.asScala) {
      val sq = elem.getSq
      val sqInOneObject = sq.getInsertStmtsInOneObject
      allInsertStmts.add(sqInOneObject)
    }
    for (s <- printOrder.asScala) {
      for (insertS <- allInsertStmts.asScala) {
        val mapIt = insertS.asScala.iterator
        while (mapIt.hasNext) {
          val elem = mapIt.next
          if (elem._1.equalsIgnoreCase(s)) {
            val tmp = elem._2
            val tmpIt = tmp.asScala.iterator
            while (tmpIt.hasNext) {
              val stmt = tmpIt.next
              insertPw.println(stmt._2)
            }
          }
        }
      }
    }
    insCL.setInsertPath(insertPath)
    insertPw.flush()
    insertPw.close()
    allInsertStmts.clear()

    // convert select statements
    val selCL = convert(selAL, impl)
    val selectPath = pathBase + File.separator + implFileName + "_select.sql"
    selCL.setSelectPath(selectPath)
    val selectFile: File = new File(selectPath)
    if (selectFile.exists()) {
      selectFile.delete()
      selectFile.createNewFile()
    } else {
      selectFile.createNewFile()
    }
    val selectPw: PrintWriter = new PrintWriter(selectFile)
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      selectPw.println("USE " + implFileName + ";")
    }
    val allSelectStmts = new util.ArrayList[util.Map[String, util.Map[Integer, String]]]()
    for (elem <- selCL.getQuerySet.asScala) {
      val sq = elem.getSq
      val sqInOneObject = sq.getSelectStmtsInOneObject
      allSelectStmts.add(sqInOneObject)
    }
    for (_ <- printOrder.asScala) {
      for (selectS <- allSelectStmts.asScala) {
        val mapIt = selectS.asScala.iterator
        while (mapIt.hasNext) {
          val elem = mapIt.next
          val stmts = elem._2.values().asScala
          for (e <- stmts) {
            selectPw.println(e)
          }
        }
      }
    }
    selCL.setSelectPath(selectPath)
    selectPw.flush()
    selectPw.close()
    allSelectStmts.clear()

    val ctmf: DBFormalConcreteTimeMeasurementFunction = new DBFormalConcreteTimeMeasurementFunction(insCL, selCL)
    concMFSet.setCtmf(ctmf)

    val csmf: DBFormalConcreteSpaceMeasurementFunction = new DBFormalConcreteSpaceMeasurementFunction(insCL)
    concMFSet.setCsmf(csmf)
    concMFSet.setImpl(impl)
    concMFSet
  }

  private def convert(absl: AbstractLoad, impl: DBImplementation): ConcreteLoad = {
    val cl: ConcreteLoad = new ConcreteLoad()

    val absqs = absl.getQuerySet
    val it = absqs.iterator()
    while (it.hasNext) {
      val absq = it.next()
      val cq = convertQuery(absq, impl)
      cl.getQuerySet.add(cq)
    }
    cl
  }

  private def convertQuery(absq: AbstractQuery, impl: DBImplementation): ConcreteQuery = {
    // get the action of absq ; a = absq.getAction()
    val cq = new ConcreteQuery()
    val a = absq.getAction
    if (a == Action.INSERT) {
      cq.setAction(Action.INSERT)
      cq.setSq(specializeInsertQuery(absq, impl, null))
    } else {
      cq.setAction(Action.SELECT)
      cq.setSq(specializeSelectQuery(absq, impl, null))
    }
    cq
  }

  private def genObjSpec(fSpec: FormalSpecificationType): ObjectSpec = {
    val fSpecPath = fSpec.getSpec
    val objSpecPath = fSpecPath.substring(0, fSpecPath.length() - 4) + "_dm.als"

    val aotad: AlloyOMToAlloyDM = new AlloyOMToAlloyDM()
    aotad.run(fSpecPath, objSpecPath, AppConfig.getIntScopeForTestCases)

    val objSpec = new ObjectSpec()

    val dbDSpec = fSpec
    objSpec.setIds(dbDSpec.getIds)
    objSpec.setAssociations(dbDSpec.getAssociations)
    objSpec.setTypeList(dbDSpec.getTypeMap)
    objSpec.setSigs(dbDSpec.getSigs)

    objSpec.setSpecPath(objSpecPath)
    objSpec
  }

  private def myTFunction(fAB: FormalAbstractMeasurementFunctionSet): List[ImplementationType] => List[FormalConcreteMeasurementFunctionSet] = {
    (_: List[ImplementationType]).map(impl => getConcreteMeasurementFunctionSet(fAB, impl))
  }

  private def createFormalSpec(spec: SpecificationType): FormalSpecificationType = {
    val dbfs = new DBFormalSpecification(spec.getSpecFile)
    dbfs.parseSpec()
    dbfs
  }

  private def myIFunction(fImp: FormalImplementationType): ImplementationType = {
    /**
     * compute FormalImplementation schema name
     * sigs here is all signatures in FormalSpecification (alloyOM), which already be set by lFunction
     * set all needed information for test cases generation here, initialize the global variable SolveAlloyDM
     */
    val fImpFileName = fImp.getImplementation
    val impFileName = fImpFileName.substring(0, fImpFileName.length() - 4) + ".sql"

    val parser = new ORMParser(fImpFileName, impFileName, fImp.getSigs)
    parser.createSchemas()
    /**
     * Need to set all needed information for test cases generation here
     */
    val impl = new DBImplementation(impFileName)
    impl.setAllFields(parser.getallFields())
    impl.setPrimaryKeys(parser.getPrimaryKeys)
    impl.setReverseTAssociate(parser.getReverseTAssociate)
    impl.setFields(parser.getFields)
    impl.setFieldsTable(parser.getFieldsTable)
    impl.setForeignKeys(parser.getForeignKey)
    impl.setDataProvider(parser.getDataProvider)
    impl.setAssociations(parser.getAssociations)
    impl.setReverseIDs(parser.getReverseIds)

    impl.setSigs(fImp.getSigs)
    impl.setIds(fImp.getIds)
    impl.setAssociationsForCreateSchemas(fImp.getAssociationsForCreateSchemas)
    impl.setTypeMap(fImp.getTypeMap)

    impl
  }

  // BFunction here is an identity function
  private def myBFunction(fCB: FormalConcreteMeasurementFunctionSet): MeasurementFunctionSetType = {
    val tLoads = fCB.getCtmf.getLoads
    val sLoads = fCB.getCsmf.getLoads

    val dbConTMF = new DBConcreteTimeMeasurementFunction(tLoads)
    val dbConSMF = new DBConcreteSpaceMeasurementFunction(sLoads)

    val dbConMF = new DBConcreteMeasurementFunctionSet(dbConTMF, dbConSMF)
    dbConMF.setImpl(fCB.getImpl)
    dbConMF
  }
}
