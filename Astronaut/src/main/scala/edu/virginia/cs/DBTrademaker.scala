package edu.virginia.cs

import edu.virginia.cs.Framework.Types.AbstractQuery.Action
import edu.virginia.cs.Framework.Types._
import edu.virginia.cs.Framework._
import edu.virginia.cs.Synthesizer._
import edu.virginia.cs.Uniq.DeleteUniq
import org.apache.spark.{SparkConf, SparkContext}

import java.io.{File, FileWriter, PrintWriter}
import java.text.{NumberFormat, ParsePosition}
import java.util.{ArrayList, HashMap, Random, UUID}
import java.util.concurrent.TimeUnit
import scala.collection.JavaConversions._

//import scala.tools.nsc.transform.SpecializeTypes.Implementation

class DBTrademaker extends AstronautFramework {

  var isDebugOn = AppConfig.getDebug
  var timeInterval: Long = 1
  var startTime: Long = 1
  var endTime: Long = 1

  type SpecificationType >: DBSpecification
  type ImplementationType >: DBImplementation
  type FormalSpecificationType >: DBFormalSpecification
  type FormalImplementationType >: DBFormalImplementation

  def run() = {

    if (isDebugOn) {
      println("hello world")
    }

    val specs = AppConfig.getSpecs
    for (spec <- specs) {

      // synthesize the tradespace for the spec
      val mySpec: DBSpecification = new DBSpecification(spec)
      val evaluatedResults = tradespaceFunction(mySpec)

      // write the result to files and print it out
      // iterate list of results
      // get head of the list 
      // get the tail of the list
      val defaultValue = Pair[ImplementationType, MeasurementResultSetType](new DBImplementation(""),
        new DBMeasurementResult(new DBTimeMeasurementResult(-1.0, -1.0), new DBSpaceMeasurementResult(-1.0)))

      val castedResults = evaluatedResults.asInstanceOf[List[Pair[ImplementationType, MeasurementResultSetType]]]
      var resultHead = hd[Pair[ImplementationType, MeasurementResultSetType]](defaultValue)(castedResults)
      var resultTail = tl[Pair[ImplementationType, MeasurementResultSetType]](castedResults)

      if (resultHead != defaultValue) {
        var implPath: String = fst(resultHead).asInstanceOf[DBImplementation].getImPath
        val startIdx = implPath.lastIndexOf(File.separator) + 1
        // get solution file name, which is like: customerOrderObjectModel_Sol_2.sql
        val tmpPath = implPath.substring(startIdx)
        val endIdx = tmpPath.indexOf("_")
        val specName = tmpPath.substring(0, endIdx)
        implPath = implPath.substring(0, implPath.lastIndexOf(File.separator))
        implPath = implPath.substring(0, implPath.lastIndexOf(File.separator) + 1)
        val resultFilePath = implPath + specName + ".txt"

        val resultFile = new File(resultFilePath)
        val pw = new PrintWriter(resultFile)

        while (resultHead != defaultValue) {
          val impl = fst(resultHead)
          val mr = snd(resultHead)

          val sol = impl.asInstanceOf[DBImplementation].getImPath
          val solName = sol.substring(sol.lastIndexOf(File.separator) + 1, sol.lastIndexOf("."))
          pw.println(solName + ":" + mr.asInstanceOf[DBMeasurementResult].getTmr().getInsertTime() + ":" +
            mr.asInstanceOf[DBMeasurementResult].getTmr().getSelectTime() + ":" +
            mr.asInstanceOf[DBMeasurementResult].getSmr().getDbSpace())

          resultHead = hd[Pair[ImplementationType, MeasurementResultSetType]](defaultValue)(resultTail)
          resultTail = tl[Pair[ImplementationType, MeasurementResultSetType]](resultTail)
        }
        pw.close()
      }
    }

    //    var mySpec: DBSpecification = new DBSpecification(AppConfig.getSpecificationPath)
    //    mySpec.setSpecFile("/Users/tang/Desktop/ORM/Parser/customerOrderObjectModel.als")

    //    var myDBTrademaker = new DBTrademaker()
    // get solutions and test results

    if (isDebugOn) {
      println("Done")
    }
  }

  def mySynthesizer(spec: SpecificationType): (List[Prod[ImplementationType, MeasurementFunctionSetType]]) = {
    println("mySynthesizer starts")
    var fSpec: FormalSpecificationType = mySFunction(spec)

    var fImpl: List[FormalImplementationType] = myCFunction(fSpec)

    var impls = myIFunctionHelper(fImpl)

    if (AppConfig.getIsRandom == 0) {
      var fAbsMF: FormalAbstractMeasurementFunctionSet = myLFunction(fSpec)
      var fConMF: List[FormalConcreteMeasurementFunctionSet] = myTFunction(fAbsMF)(impls)
      var mfs = myBFunctionHelper(fConMF)
      var zipped = combine(impls)(mfs)
      return zipped
    } else if (AppConfig.getIsRandom == 1) {
      // get concrete measurement function by random generator
      var mfs = genRandomConcreteMF(impls)
      // Chong: copy self-defined list to Scala list while reversed
      var defaultValue = Nil[MeasurementFunctionSetType]()
      var head = hd[MeasurementFunctionSetType](defaultValue)(mfs)
      var tail = tl[MeasurementFunctionSetType](mfs)

      var reversedMfs: List[MeasurementFunctionSetType] = Nil[MeasurementFunctionSetType]()
      while (head != defaultValue) {
        reversedMfs = Cons[MeasurementFunctionSetType](head, reversedMfs)
        head = hd[MeasurementFunctionSetType](defaultValue)(tail)
        tail = tl[MeasurementFunctionSetType](tail)
      }

      // iterate to create list of pairs, call "combine" will result in StackOverFlowError
      var zipped = combine(impls)(reversedMfs)
      return zipped
    }
    null
  }

  def genRandomConcreteMF(impls: List[ImplementationType]): List[MeasurementFunctionSetType] = {
    if (isDebugOn) {
      println("genRandomConcreteMF starts")
    }

    // create DBConcreteMeasurementFunctionSet for each of DBImplementation in impls
    /**
     * convert List[ImplementationType] to ArrayList[DBImplementation]
     * iterate all implementations and create DBMeasurementFunction
     */
    var mfSets: List[MeasurementFunctionSetType] = Nil()

    var dbImpls: ArrayList[DBImplementation] = new ArrayList
    var defaultValue: ImplementationType = Option.empty[ImplementationType].orNull // = new ImplementationType
    var head = hd[ImplementationType](defaultValue)(impls)
    var tail = tl[ImplementationType](impls)
    while (head != defaultValue) {
      dbImpls.add(head.asInstanceOf[DBImplementation])
      head = hd[ImplementationType](defaultValue)(tail)
      tail = tl[ImplementationType](tail)
    }

    // create random instance range by range
    var subRange = AppConfig.getSubRange
    var range = AppConfig.getRandomRange
    //    for (i <- 0 to range by subRange) {
    //      var lowRange = i + 1
    //      var highRange = -1
    //      if (i + subRange > range) {
    //        highRange = range
    //      } else {
    //        highRange = i + subRange
    //      }

    var allInstances = generateRandomInstances(dbImpls.get(0).getSigs(), dbImpls.get(0).getTypeMap(), 1, range)
    var implIt = dbImpls.iterator()
    var i = 0
    while (implIt.hasNext()) {
      i = i + 1
      var singleImpl = implIt.next()
      // create abstract loads for impl
      // call getRandomAbsMeasurementFunctionSet for each impl
      // call getConcreteMeasurementFunctionSets for each abstract load
      //      var timeLoads: ArrayList[ConcreteLoad] = new ArrayList[ConcreteLoad](2)
      //      var spaceLoads: ArrayList[ConcreteLoad] = new ArrayList[ConcreteLoad](1)

      // generate statements sub range by sub range and appending to files
      //      var insertLoad: ConcreteLoad = generateRandomInsertStatements(singleImpl, allInstances)
      //      var selectLoad: ConcreteLoad = generateRandomSelectStatements(singleImpl, allInstances)

      // create measurement functions after generating the last segment of test cases
      //        if (highRange == range) {
      //          timeLoads.add(insertLoad)
      //          timeLoads.add(selectLoad)
      //          
      //          spaceLoads.add(insertLoad)

      //          var ctmf: DBConcreteTimeMeasurementFunction = new DBConcreteTimeMeasurementFunction(timeLoads)
      var ctmf: DBConcreteTimeMeasurementFunction = new DBConcreteTimeMeasurementFunction()
      ctmf.setInstances(allInstances)
      ctmf.setImpl(singleImpl)
      // create select statements      
      //          var csmf: DBConcreteSpaceMeasurementFunction = new DBConcreteSpaceMeasurementFunction(spaceLoads)
      var csmf: DBConcreteSpaceMeasurementFunction = new DBConcreteSpaceMeasurementFunction()
      csmf.setInstances(allInstances)
      csmf.setImpl(singleImpl)

      var mfs: DBConcreteMeasurementFunctionSet = new DBConcreteMeasurementFunctionSet(ctmf, csmf)
      mfSets = Cons[MeasurementFunctionSetType](mfs, mfSets)
      //println("add all instances to impl " + i)
      //        }
    }
    //    }
    if (isDebugOn) {
      println("genRandomConcreteMF ends")
    }

    return mfSets
  }

  // get the random instances, and return insertFilePath
  def generateRandomInsertStatements(impl: DBImplementation, instances: HashMap[String, HashMap[String, ArrayList[CodeNamePair]]]): ConcreteLoad = {
    var insertSpecializedQuery: SpecializedQuery = specializeInsertQuery(null, impl, instances)
    var cq = new ConcreteQuery()
    cq.setAction(Action.INSERT)
    cq.setSq(insertSpecializedQuery)
    var cqs = new ArrayList[ConcreteQuery](1)
    cqs.add(cq)
    /**
     * print out insert scripts
     */
    var insCL = new ConcreteLoad()
    insCL.setQuerySet(cqs)

    var implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    var implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs();
    }
    var insertPath = pathBase + File.separator + implFileName + "_insert.sql"
    //    var compressedInsertPath = pathBase + File.separator + implFileName + "_insert.sql.tar.gz"
    //    var tmpInsertPath = pathBase + File.separator + implFileName + "_insert_tmp.sql"
    insCL.setInsertPath(insertPath)
    insCL.setSelectPath("")
    var insertFile: File = new File(insertPath)
    if (!insertFile.exists()) {
      insertFile.createNewFile()
    }

    val insertPw: PrintWriter = new PrintWriter(new FileWriter(insertPath, true))
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      insertPw.println("USE " + implFileName + ";")
    } else if (AppConfig.getTestDB.equalsIgnoreCase("postgres")) {
      insertPw.println("BEGIN;")
    }
    var allInsertStmts = new ArrayList[HashMap[String, HashMap[Integer, String]]]()
    for (elem <- insCL.getQuerySet()) {
      val sq = elem.getSq()
      val sqInOneObject = sq.getInsertStmtsInOneObject()
      allInsertStmts.add(sqInOneObject)
    }

    val printOrder = PrintOrder.getOutPutOrders(pathBase)
    for (s <- printOrder) {
      for (insertS <- allInsertStmts) {
        val mapIt = insertS.iterator
        while (mapIt.hasNext) {
          val table = mapIt.next // (String, HashMap[Integer, String]) = (tableName, HashMap[ID, Statements])
          if (table._1.equalsIgnoreCase(s)) { // check table name
            val stmt = table._2
            val stmtIt = stmt.iterator
            while (stmtIt.hasNext) {
              val idStmt = stmtIt.next
              val stmtStr: String = String.valueOf(idStmt._2.toCharArray())
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

    return insCL
  }

  // get the random instances, and return insertFilePath
  def generateRandomSelectStatements(impl: DBImplementation, instances: HashMap[String, HashMap[String, ArrayList[CodeNamePair]]]): ConcreteLoad = {
    var selectSpecializedQuery: SpecializedQuery = specializeSelectQuery(null, impl, instances)

    var cq = new ConcreteQuery()
    cq.setAction(Action.SELECT)
    cq.setSq(selectSpecializedQuery)
    var cqs = new ArrayList[ConcreteQuery]()
    cqs.add(cq)
    var selCL = new ConcreteLoad()
    selCL.setQuerySet(cqs)

    // convert select statements

    var implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    var implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs();
    }
    val printOrder = PrintOrder.getOutPutOrders(pathBase)

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
    val allSelectStmts = new ArrayList[HashMap[String, HashMap[Integer, String]]]()

    for (elem <- selCL.getQuerySet()) {
      val sq = elem.getSq()
      val sqInOneObject = sq.getSelectStmtsInOneObject()
      allSelectStmts.add(sqInOneObject)
    }

    for (s: String <- printOrder) {
      for (selectS <- allSelectStmts) {
        val mapIt = selectS.iterator
        while (mapIt.hasNext) {
          val elem = mapIt.next // (String, HashMap[Integer, String]) = (tableName, HashMap[ID, Statements])
          if (elem._1.equalsIgnoreCase(s)) {
            val tmp = elem._2
            val tmpIt = tmp.iterator
            while (tmpIt.hasNext) {
              val stmt = tmpIt.next
              val stmtStr = String.valueOf(stmt._2.toCharArray())
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

    return selCL
  }

  //  def generateRandomInstances(impl: DBImplementation, low: Integer, high: Integer): HashMap[String, HashMap[String, ArrayList[CodeNamePair]]] = {
  def generateRandomInstances(sigs: ArrayList[Sig], types: HashMap[String, String], low: Integer, high: Integer): HashMap[String, HashMap[String, ArrayList[CodeNamePair]]] = {
    if (isDebugOn) {
      println("Random Instance generator starts....")
    }

    var instances = new HashMap[String, HashMap[String, ArrayList[CodeNamePair]]](5)

    var randomRange = AppConfig.getRandomRange

    var lowValue = low.intValue()
    var highValue = high.intValue()
    var range = highValue - lowValue
    for (sig <- sigs) {
      for (i <- lowValue to highValue) {
        var sigName = sig.getSigName()
        var instanceName = sigName + i;
        if (!instances.containsKey(sigName)) {
          instances.put(sigName, new HashMap[String, ArrayList[CodeNamePair]](3))
        }
        if (!instances.get(sigName).containsKey(instanceName)) {
          instances.get(sigName).put(instanceName, new ArrayList[CodeNamePair](3))
        }

        if (sig.getCategory() == 0) { // 0 is class
          var id: String = sig.getId()
          var fieldValue: String = String.valueOf(i)
          instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + id, fieldValue))
          for (fieldName <- sig.getAttrSet()) {
            if (!fieldName.equalsIgnoreCase(id)) {
              var fieldType: String = types.get(fieldName)
              if (fieldType.contains("Int")) {
                var rand: Random = new Random(System.currentTimeMillis())
                fieldValue = String.valueOf(lowValue + rand.nextInt(range));
              } else if (fieldType.equalsIgnoreCase("string")) {
                fieldValue = fieldName + UUID.randomUUID().toString().substring(0, 6)
              } else if (fieldType.equalsIgnoreCase("Bool")) {
                // assign it as true
                fieldValue = "true";
              }
              instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + fieldName, fieldValue));
            }
          }
        } else if (sig.getCategory() == 1) { // 1 is association
          var rand: Random = new Random(System.currentTimeMillis());
          var srcIDName: String = getIDBySigName(sigs, sig.getSrc());

          var srcIDValue = i;
          var dstIDName = getIDBySigName(sigs, sig.getDst());

          var dstIDValue = i;
          instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + srcIDName, String.valueOf(srcIDValue)));
          instances.get(sigName).get(instanceName).add(new CodeNamePair(sigName + "_" + dstIDName, String.valueOf(dstIDValue)));
        }
      }
    }
    if (isDebugOn) {
      println("Random Instance generator ends....")
    }
    return instances
  }

  def myRunBenchmark(prod: Prod[ImplementationType, MeasurementFunctionSetType]): Prod[ImplementationType, MeasurementResultSetType] = {
    if (isDebugOn) {
      println("============================================================================")
      println("==================This is myRunBenchmark function===========================")
      println("============================================================================")
    }

    //    var myPair: Pair[ImplementationType, MeasurementResultSetType] =
    //      Pair[ImplementationType, MeasurementResultSetType](new DBImplementation(""),
    //        new DBConcreteMeasurementFunctionSet(new DBConcreteTimeMeasurementFunction(), new DBConcreteSpaceMeasurementFunction()))

    var impl = fst(prod).asInstanceOf[DBImplementation]
    var mfs = snd(prod).asInstanceOf[DBConcreteMeasurementFunctionSet]
    mfs.setImpl(impl)

    // If benchmark is random test loads, need to create concrete load first and then run them
    if (AppConfig.getIsRandom == 1) {
      // call concrete loads creator here
      var timeLoads: ArrayList[ConcreteLoad] = new ArrayList[ConcreteLoad](2)
      var spaceLoads: ArrayList[ConcreteLoad] = new ArrayList[ConcreteLoad](1)
      var insertLoad: ConcreteLoad = generateRandomInsertStatements(impl, mfs.getCtmf().getInstances())
      var selectLoad: ConcreteLoad = generateRandomSelectStatements(impl, mfs.getCtmf().getInstances())
      timeLoads.add(insertLoad)
      timeLoads.add(selectLoad)

      spaceLoads.add(insertLoad)
      mfs.getCtmf().setLoads(timeLoads)
      mfs.getCsmf().setLoads(spaceLoads)
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
    var tmf = mfs.getCtmf()
    tmf.setImpl(impl)
    var smf = mfs.getCsmf()
    smf.setImpl(impl)
    if (isDebugOn) {
      println("=======================")
      println("TimeMeasurementFunction")
      println("=======================")
    }
    var tmr = tmf.run()
    println("Insert time: " + tmr.getInsertTime + "s. Select time: " + tmr.getSelectTime + "s.")
    if (isDebugOn) {
      println("=======================")
      println("SpaceMeasurementFunction")
      println("=======================")
    }
    var smr = smf.run()
    if (isDebugOn) {
      println("DB space: " + smr.getDbSpace + "kb")
    }

    var dbmr = new DBMeasurementResult(tmr, smr)
    dbmr.setImpl(impl)
    val myPair = Pair[ImplementationType, MeasurementResultSetType](impl, dbmr)
    //    }

    // delete insert and select files
    var loads: ArrayList[ConcreteLoad] = mfs.getCtmf.getLoads
    for (load <- loads) {
      var inPath = load.getInsertPath
      var slPath = load.getSelectPath

      var inFile = new File(inPath)
      if (inFile.exists()) {
        //        Chong:
        inFile.delete()
      }
      var slFile = new File(slPath)
      if (slFile.exists()) {
        //        Chong:
        slFile.delete()
      }
    }

    if (isDebugOn) {
      println("finish implementation: " + impl.getImPath() + ":" + dbmr.getTmr.getInsertTime + "," + dbmr.getTmr.getSelectTime + "," + dbmr.getSmr.getDbSpace)
    }

    // we can write the results into hadoop file system
    // /trademaker/modelName/solutionName

    Pair[ImplementationType, MeasurementResultSetType](new DBImplementation(impl.getImPath()), dbmr)

    //    return myPair
  }

  // analyze and tradespace are already defined in Tradespace specification
  // we can call "tradespace" function to synthesize implementation and benchmark
  var myTradespace: Tradespace = new Build_Tradespace(mySynthesizer _, myRunBenchmark _, myMapReduce _)

  // here we get the tradespace function and analyze function
  def tradespaceFunction = tradespace(myTradespace) // fun: (SpecificationType => List[Prod[ImplementationType, BenchmarkResultType]])

  /**
   * Use Spark to evaluate solutions
   *
   * @param list
   * @return list of implementation and measurement result
   */
  def myMapReduce(list: List[Prod[ImplementationType, MeasurementFunctionSetType]]): List[Prod[ImplementationType, MeasurementResultSetType]] = {
    if (isDebugOn) {
      println("This is myMapReduce function")
    }

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
      .set("spark.storage.memoryFraction", "0.9");

    //      .setMaster("spark://centurion002.cs.virginia.edu:7077")
    //      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    //      .set("spark.kryo.registrator", "edu.virginia.cs.MyRegistrator")
    //      .set("spark.kryoserializer.buffer.mb", "512")

    val sc = new SparkContext(conf)
    /**
     * need to convert List and Prod to scala Array and Tuple2
     * 1. iterate List to create Array, while convert Prod to Tuple2
     */
    var newList: Array[(DBImplementation, DBConcreteMeasurementFunctionSet)] =
      Array[(DBImplementation, DBConcreteMeasurementFunctionSet)]()
    val defaultValue = Pair[ImplementationType, MeasurementFunctionSetType](new DBImplementation(""),
      new DBConcreteMeasurementFunctionSet(new DBConcreteTimeMeasurementFunction(), new DBConcreteSpaceMeasurementFunction()))
    var head = hd[Prod[ImplementationType, MeasurementFunctionSetType]](defaultValue)(list)
    var tail = tl[Prod[ImplementationType, MeasurementFunctionSetType]](list)
    while (head != defaultValue) {
      val tmpTuple = (fst[ImplementationType, MeasurementFunctionSetType](head).asInstanceOf[DBImplementation],
        snd[ImplementationType, MeasurementFunctionSetType](head).asInstanceOf[DBConcreteMeasurementFunctionSet])
      newList = newList :+ tmpTuple
      head = hd[Prod[ImplementationType, MeasurementFunctionSetType]](defaultValue)(tail)
      tail = tl[Prod[ImplementationType, MeasurementFunctionSetType]](tail)
    }
    val rdd = sc.parallelize(newList)
    //    println("newList_RDD count: "+rdd.count())
    val evaluationResult = rdd.map(e => {
      // construct Prod from e
      val prod = Pair[ImplementationType, MeasurementFunctionSetType](e._1, e._2)
      val result = myRunBenchmark(prod)
      result
    })

    //    evaluationResult.foreach()
    if (isDebugOn) {
      println("Finish execute! Go to collect()")
    }

    val collectedResult = evaluationResult.collect()

    if (isDebugOn) {
      println("Finish collect()")
    }

    endTime = System.currentTimeMillis
    timeInterval = endTime - startTime

    if (isDebugOn) {
      println("Spend time: " + TimeUnit.MILLISECONDS.toSeconds(timeInterval))
    }

    var resultList: List[Prod[ImplementationType, MeasurementResultSetType]] = Nil[Prod[ImplementationType, MeasurementResultSetType]]()
    collectedResult.map(e => {
      resultList = Cons[Prod[ImplementationType, MeasurementResultSetType]](e, resultList)
    })

    //    val result = evaluationResult.toLocalIterator
    //
    //    var resultList:List[Prod[ImplementationType, MeasurementResultSetType]] = Nil[Prod[ImplementationType, MeasurementResultSetType]]()
    //
    //    result.foreach(e => {
    //      resultList = Cons[Prod[ImplementationType, MeasurementResultSetType]](e, resultList)
    //    })

    //    for(r <- result){
    //      resultList = Cons[Prod[ImplementationType, MeasurementResultSetType]](r, resultList)
    //    }
    if (isDebugOn) {
      println("====================================================")
      println("====================================================")
    }

    sc.stop()
    resultList
  }

  /**
   * Stub out ParetoFront
   */
  def myParetoFilter(list: List[Prod[ImplementationType, MeasurementResultSetType]]): List[Prod[ImplementationType, MeasurementResultSetType]] = {
    if (isDebugOn) {
      println("This is myParetoFilter function")
    }

    // TODO:    currently, we implemented pareto optimal filter in python
    list
  }

  var myParetoFront: ParetoFront = new Build_ParetoFront(myTradespace, myParetoFilter)

  // returns the pareto function we defined in the specification
  //  def myParetoFrontFunction = paretoFront(myParetoFront)

  //  def getParetoResults(mySpec: SpecificationType) = myParetoFrontFunction(mySpec)

  /**
   * Define functions for Trademaker in specification
   */
  def myCFunction(fSpec: FormalSpecificationType): List[FormalImplementationType] = {
    if (isDebugOn) {
      println("This is myCFunction function")
    }
    var solFolder: String = AppConfig.getSolutionFolder
    if (solFolder == null || solFolder.trim.isEmpty) {
      startTime = System.currentTimeMillis()
      /*
       * get specification file path
       * calculate solution folder based on specification file path
       * call smartbridge() function to synthesize formal implementations
       * scan solution folder to get implementations
       */
      val specPath: String = fSpec.asInstanceOf[DBFormalSpecification].getSpec
      solFolder = specPath.substring(0, specPath.lastIndexOf(File.separator))
      val alloyOMName = specPath.substring(specPath.lastIndexOf(File.separator) + 1, specPath.lastIndexOf("."))
      solFolder = solFolder + File.separator + alloyOMName + File.separator + "ImplSolution"
      recursiveDelete(new File(solFolder))
      if (!new File(solFolder).exists()) {
        val fileFP = new File(solFolder)
        val rtn = fileFP.mkdirs()
        if (rtn) {
          println("Solution folder created!:::" + solFolder);
        } else {
          if (isDebugOn) {
            println("Create solution folder failed!");
          }
        }
      }

      // get mapping run file
      val mappingRun: String = FileOperation.getMappingRun(specPath)
      // call smartBridge
      new SmartBridge(solFolder, mappingRun, AppConfig.getMaxSolForImpl.intValue());

      // delete duplicate solutions
      DeleteUniq.del(solFolder)

      // log the time taken to synthesize all the results
      var synthTime = System.currentTimeMillis() - startTime;
      println(s"[ICSE2022] synthesis complete: time=$synthTime, spec=$specPath")
    } else {
      println(s"[ICSE2020] reading results from folder: $solFolder")
    }

    getDatabaseImplsForFolder(fSpec, solFolder)
  }

  def getDatabaseImplsForFolder(fSpec: FormalSpecificationType, solFolder: String): List[FormalImplementationType] = {
    // scan solution folder, and get all the solutions
    var solFiles: Array[File] = new java.io.File(solFolder).listFiles.filter(_.getName.endsWith(".xml"))
    var imFile: File = null
    var implList: List[FormalImplementationType] = Nil[FormalImplementationType]()

    for (file <- solFiles) {
      var imFileName = file.getName()
      var dbImpl: DBFormalImplementation = new DBFormalImplementation()
      dbImpl.setImp(file.getAbsolutePath)
      dbImpl.setSigs(fSpec.asInstanceOf[DBFormalSpecification].getSigs)
      dbImpl.setAssociationsForCreateSchemas(fSpec.asInstanceOf[DBFormalSpecification].getAssociations)
      dbImpl.setTypeMap(fSpec.asInstanceOf[DBFormalSpecification].getTypeMap)
      dbImpl.setIds(fSpec.asInstanceOf[DBFormalSpecification].getIds)
      implList = new Cons[FormalImplementationType](dbImpl, implList)
    }
    implList
  }

  def myAFunction(fImp: FormalImplementationType): FormalSpecificationType = {
    if (isDebugOn) {
      println("This is myAFunction function")
    }
    ""
  }

  def myLFunction(fSpec: FormalSpecificationType): FormalAbstractMeasurementFunctionSet = {
    // generate two abstract load objects:  insert and select
    // create two measurement functions, one for time, one for space
    // wrap them in a FormalAbstractMeasurementFunctionSet

    var absLoads: FormalAbstractLoadSet = null

    absLoads = generateFormalAbstractLoadSet(fSpec)

    var absTimeMeasurementFunction = new DBFormalAbstractTimeMeasurementFunction(absLoads.getInsLoad(), absLoads.getSelLoad())
    var absSpaceMeasurementFunction = new DBFormalAbstractSpaceMeasurementFunction(absLoads.getInsLoad())
    new DBFormalAbstractMeasurementFunctionSet(absTimeMeasurementFunction, absSpaceMeasurementFunction)
  }

  def getIDBySigName(sigs: ArrayList[Sig], sigName: String): String = {
    var pk: String = "";
    for (s <- sigs) {
      if (s.getSigName().equalsIgnoreCase(sigName)) {
        return s.getId();
      }
    }
    return pk;
  }

  def recursiveDelete(f: File): Boolean = {
    if (f.isDirectory()) {
      for (c <- f.listFiles())
        recursiveDelete(c);
    }
    if (!f.delete()) {
      //      throw new FileNotFoundException("Failed to delete file: " + f)
    }
    true
  }

  def generateFormalAbstractLoadSet(fSpec: FormalSpecificationType): FormalAbstractLoadSet = {

    // initialize two empty abstract loads objects, to contain insert and select queries
    // for each object solution to fSpec
    //     - generate abstract insert and select queries,
    //			and add them to the corresponding abstract load objects
    // package up the two abstract load objects in a FormalAbstractLoadSet object and return the result

    // initialize two empty abstract loads objects, to contain insert and select queries
    var insAbsLoad = new AbstractLoad()
    var selAbsLoad = new AbstractLoad()

    // use Alloy analyzer
    // for each object solution to fSpec
    var objSpec = genObjSpec(fSpec)

    // construct path to solution folder
    var specPath = objSpec.getSpecPath()
    var lenOfExtension = "_dm.als".length()
    var objectSolFolder = specPath.substring(0, specPath.length() - lenOfExtension)

    objectSolFolder = objectSolFolder + File.separator + "TestSolutions"
    recursiveDelete(new File(objectSolFolder))
    new File(objectSolFolder).mkdirs()

    // call objects generator
    var loadSynthesizer = new LoadSynthesizer()
    var startTime = System.currentTimeMillis()
    loadSynthesizer.genObjsHelper(specPath, objectSolFolder, fSpec.asInstanceOf[DBFormalSpecification].getIds) // parse ID for negation

    /*
     * get solutions to alloy spec (stored as XML files)
     *
     * for each such solution ("object")
     *    * generate two abstract queries
     *    * add queries to relevant abstract load objects
     */

    // iterate all objects
    var objectFiles: Array[File] = new java.io.File(objectSolFolder).listFiles.filter(_.getName.endsWith(".xml"))
    var file: File = null
    var objects: ObjectSet = new ObjectSet()

    var insQuerySet: ArrayList[AbstractQuery] = new ArrayList()
    var selQuerySet: ArrayList[AbstractQuery] = new ArrayList()

    for (file <- objectFiles) {
      var objectFileName = file.getName()
      var singleObject = new ObjectOfDM(file.getAbsolutePath())

      var insQuery: AbstractQuery = new AbstractQuery(AbstractQuery.Action.INSERT, singleObject)
      var selQuery: AbstractQuery = new AbstractQuery(AbstractQuery.Action.SELECT, singleObject)

      insQuerySet.add(insQuery)
      selQuerySet.add(selQuery)
    }
    insAbsLoad.setQuerySet(insQuerySet)
    selAbsLoad.setQuerySet(selQuerySet)

    var endTime = System.currentTimeMillis()
    var interval = endTime - startTime
    if (isDebugOn) {
      var spec = objSpec.getSpecPath()
      spec = spec.substring(spec.lastIndexOf(File.separator), spec.indexOf("."))
      println("generate objects for: " + spec)
    }

    var fAbsLoadSet: FormalAbstractLoadSet = new FormalAbstractLoadSet(insAbsLoad, selAbsLoad)
    return fAbsLoadSet
  }

  /*
   *  purpose is to convert a given abstract measurement function (set of insert or select abstract queries) into a concrete measurement
   *  function, specialized to a particular implementation.
  */

  def getConcreteMeasurementFunctionSets(absMF: DBFormalAbstractMeasurementFunctionSet, impls: ArrayList[DBImplementation]): ArrayList[DBFormalConcreteMeasurementFunctionSet] = {
    // iterate over all abstract measurement functions and convert them all to concrete measurement functions
    var returnValue: ArrayList[DBFormalConcreteMeasurementFunctionSet] = new ArrayList()
    var i: DBFormalImplementation = null
    var it = impls.iterator()
    while (it.hasNext()) {
      var nextImpl = it.next()
      var startTime = System.currentTimeMillis()

      returnValue.add(getConcreteMeasurementFunctionSet(absMF, nextImpl))
      var endTime = System.currentTimeMillis()
      if (isDebugOn) {
        var solName = nextImpl.asInstanceOf[DBImplementation].getImPath
        solName = solName.substring(solName.lastIndexOf(File.separator) + 1, solName.indexOf("."))
        val df = new java.text.SimpleDateFormat("HH:mm:ss")
        println("generate concrete MF for solution: " + solName)
        //        println("took time: " + df.format(endTime - startTime) + "s")
      }
    }
    returnValue
  }

  def getConcreteMeasurementFunctionSet(absMF: DBFormalAbstractMeasurementFunctionSet, impl: DBImplementation): DBFormalConcreteMeasurementFunctionSet = {
    var concMFSet: DBFormalConcreteMeasurementFunctionSet = new DBFormalConcreteMeasurementFunctionSet()

    // for time
    var tmf: DBFormalAbstractMeasurementFunction = absMF.getTmf()
    var tmfALoads: ArrayList[AbstractLoad] = tmf.getLoads()
    var insAL: AbstractLoad = tmfALoads.get(0)
    var selAL: AbstractLoad = tmfALoads.get(1)
    var insCL = convert(insAL, impl)
    /**
     * print out insert scripts
     */
    var implPath = impl.getImPath
    var pathBase = implPath.substring(0, implPath.lastIndexOf(File.separator))
    var implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    pathBase += File.separator + "TestCases"
    if (!new File(pathBase).exists()) {
      new File(pathBase).mkdirs();
    }
    var insertPath = pathBase + File.separator + implFileName + "_insert.sql"
    insCL.setInsertPath(insertPath)
    var insertFile: File = new File(insertPath)
    if (insertFile.exists()) {
      insertFile.delete()
      insertFile.createNewFile()
    } else {
      insertFile.createNewFile()
    }
    var insertPw: PrintWriter = new PrintWriter(insertFile)
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      insertPw.println("USE " + implFileName + ";")
    }
    var allInsertStmts = new ArrayList[HashMap[String, HashMap[Integer, String]]]();
    val printOrder = PrintOrder.getOutPutOrders(pathBase)
    for (elem <- insCL.getQuerySet()) {
      var sq = elem.getSq()
      var sqInOneObject = sq.getInsertStmtsInOneObject()
      allInsertStmts.add(sqInOneObject)
    }
    for (s <- printOrder) {
      for (insertS <- allInsertStmts) {
        var mapIt = insertS.iterator
        while (mapIt.hasNext) {
          var elem = mapIt.next // (String, HashMap[Integer, String]) = (tableName, HashMap[ID, Statements])
          if (elem._1.equalsIgnoreCase(s)) {
            var tmp = elem._2
            var tmpIt = tmp.iterator
            while (tmpIt.hasNext) {
              var stmt = tmpIt.next
              insertPw.println(stmt._2)
            }
          }
        }
      }
    }
    insCL.setInsertPath(insertPath)
    insertPw.flush()
    insertPw.close()
    // call shell command to remove duplicated lines,
    // and write results back to tmp.sql file
    //    var tmpFiles = pathBase + File.separator + "tmp.sql"
    //    var strCmd = "awk '!x[$0]++' " + insertPath
    //    (Process(strCmd) #> new File(tmpFiles)).!
    // remove duplicate lines
    // (Process(Seq("awk", "!x[$0]++", insertPath)) #> new File(tmpFiles)).!
    //    var tmpFiles1 = tmpFiles + "1"
    //    // add "FLUSH TABLES;" after each line
    //    (Process(Seq("awk", "1;!(NR%1){print \"FLUSH TABLES;\";}", tmpFiles)) #> new File(tmpFiles1)).!
    // mv tmp file to insert file
    //    Process(Seq("mv", tmpFiles1, insertPath)).!
    // Process(Seq("mv", tmpFiles, insertPath)).!
    //    Process(Seq("rm", tmpFiles)).!
    allInsertStmts.clear()

    // convert select statements
    var selCL = convert(selAL, impl)
    var selectPath = pathBase + File.separator + implFileName + "_select.sql"
    selCL.setSelectPath(selectPath)
    var selectFile: File = new File(selectPath)
    if (selectFile.exists()) {
      selectFile.delete()
      selectFile.createNewFile()
    } else {
      selectFile.createNewFile()
    }
    var selectPw: PrintWriter = new PrintWriter(selectFile)
    if (AppConfig.getTestDB.equalsIgnoreCase("mysql")) {
      selectPw.println("USE " + implFileName + ";")
    }
    var allSelectStmts = new ArrayList[HashMap[String, HashMap[Integer, String]]]();
    for (elem <- selCL.getQuerySet()) {
      var sq = elem.getSq()
      var sqInOneObject = sq.getSelectStmtsInOneObject()
      allSelectStmts.add(sqInOneObject)
    }
    for (s <- printOrder) {
      for (selectS <- allSelectStmts) {
        var mapIt = selectS.iterator
        while (mapIt.hasNext) {
          var elem = mapIt.next
          var stmts = elem._2.values()
          for (e <- stmts) {
            selectPw.println(e)
          }
        }
      }
    }
    selCL.setSelectPath(selectPath)
    selectPw.flush()
    selectPw.close()

    // call shell command to remove duplicated lines,
    // and write results back to tmp.sql file
    //    tmpFiles = pathBase + File.separator + "tmp.sql"
    //    strCmd = "awk '!x[$0]++' " + selectPath
    //    (Process(strCmd) #> new File(tmpFiles)).!
    //    remove duplicate lines
    //    var tmpFiles2 = tmpFiles + "2"
    //    (Process(Seq("awk", "!x[$0]++", selectPath)) #> new File(tmpFiles)).!
    //    add "RESET QUERY CACHE;" after each line
    //    (Process(Seq("awk", "1;!(NR%1){print \"RESET QUERY CACHE;\";}", tmpFiles)) #> new File(tmpFiles2)).!
    // mv tmp file to insert file
    //    Process(Seq("mv", tmpFiles2, selectPath)).!
    //    Process(Seq("mv", tmpFiles, selectPath)).!
    //    Process(Seq("rm", tmpFiles)).!
    allSelectStmts.clear()

    var ctmf: DBFormalConcreteTimeMeasurementFunction = new DBFormalConcreteTimeMeasurementFunction(insCL, selCL)
    concMFSet.setCtmf(ctmf)

    // chong: duplicated code here
    // for space
    //    var smf: DBFormalAbstractMeasurementFunction = absMF.getSmf()
    //    var smfALoads: ArrayList[AbstractLoad] = smf.getLoads()
    //    insAL = smfALoads.get(0)
    //    insCL = convert(insAL, impl)
    var csmf: DBFormalConcreteSpaceMeasurementFunction = new DBFormalConcreteSpaceMeasurementFunction(insCL)
    concMFSet.setCsmf(csmf)
    concMFSet.setImpl(impl)

    // return concMFSet
    concMFSet
  }

  def convert(absl: AbstractLoad, impl: DBImplementation): ConcreteLoad = {
    // iterate absl
    // get abstract queries in absl
    // convert abstract queries to concrete queryies
    // add concrete queries to concrete loads
    // return it

    var cl: ConcreteLoad = new ConcreteLoad()

    var absqs = absl.getQuerySet()
    var it = absqs.iterator()
    while (it.hasNext()) {
      var absq = it.next()
      var cq = convertQuery(absq, impl)
      cl.getQuerySet().add(cq)
    }
    cl
  }

  def convertQuery(absq: AbstractQuery, impl: DBImplementation): ConcreteQuery = {
    // get the action of absq ; a = absq.getAction()
    /*
     *  if a==Insert
     *  	* generateInsert()
     *   else generateSelect()
     */
    var cq = new ConcreteQuery()
    var a = absq.getAction()
    if (a == Action.INSERT) {
      cq.setAction(Action.INSERT)
      cq.setSq(specializeInsertQuery(absq, impl, null))
    } else {
      cq.setAction(Action.SELECT)
      cq.setSq(specializeSelectQuery(absq, impl, null))
    }
    cq
  }

  def specializeInsertQuery(absq: AbstractQuery, impl: DBImplementation, ins: HashMap[String, HashMap[String, ArrayList[CodeNamePair]]]): SpecializedQuery = {
    // allInstances here contains all instances in a single object file, which is got by parse the object file
    // some fields may have more than one instance
    // allInstances is a hashmap: HashMap[String, HashMap[String, ArrayList[CodeNamePair[String>>>>
    // HashMap[tableName, HashMap[instanceName, fields_value_pairs]]
    var allInstances = new HashMap[String, HashMap[String, ArrayList[CodeNamePair]]](1)

    if (absq != null) {
      allInstances = absq.getOodm().parseDocument()
    } else {
      allInstances = ins
    }

    var allInstancesIt = allInstances.entrySet().iterator()

    var field_part: String = ""
    var value_part: String = ""
    var fTables: ArrayList[String] = new ArrayList()
    var asss: ArrayList[HashMap[String, CodeNamePair]] = new ArrayList[HashMap[String, CodeNamePair]]()
    var ass: HashMap[String, CodeNamePair] = null

    var allInsertStmts: HashMap[String, HashMap[Integer, String]] = new HashMap[String, HashMap[Integer, String]]()
    /**
     * Prepare output file by implPath
     */
    var implPath = impl.getImPath
    var insertPath = implPath.substring(0, implPath.lastIndexOf(File.separator))
    var implFileName = implPath.substring(implPath.lastIndexOf(File.separator) + 1, implPath.lastIndexOf("."))
    insertPath += File.separator + "TestCases"
    if (!new File(insertPath).exists()) {
      new File(insertPath).mkdirs();
    }
    insertPath += File.separator + implFileName + "_insert.sql"

    while (allInstancesIt.hasNext) { // iterate all instances in an object
      var instances = allInstancesIt.next
      var className: String = instances.getKey // get key. tableName
      var instancesIt = instances.getValue.iterator
      while (instancesIt.hasNext) { // iterate all instances for one class
        var singleInstance = instancesIt.next
        var instanceName: String = singleInstance._1
        var fieldValuePairs = singleInstance._2
        // key is tableName, and value is fields in the table
        var dbScheme = impl.getDataSchemas
        // which table the element class will be
        var reverseTAss = impl.getReverseTAssociate
        var goToTable = getTableNameByClassName(reverseTAss, className) // null if not found
        if (goToTable != null) {
          /* there is no t_association information for this element
           * which indicates it's an one-to-many association,
           * and the information is in dst table,
           * we don't need to consider since it will be taken in find foreign key value
           */
          var id: String = getPrimaryKeyByTableName(dbScheme, goToTable)
          var id_value: String = getFieldValue(fieldValuePairs, id, impl.getTypeMap())

          field_part = "";
          value_part = "";

          var allAboutSchema: ArrayList[CodeNamePair] = dbScheme.get(goToTable)
          if (!isClassAssociate(impl, className)) {
            for (pair <- allAboutSchema if pair.getFirst().equalsIgnoreCase("fields")) {
              /**
               * check pair.getSecond() (fieldName) is in "attr"
               * if fieldName is in attr, call getFieldValue()
               * if fieldName is foreign key, iterate ids and attr
               * to get the primary class (eg, DecisionSpace)
               * if fieldName is DType, set fieldValue as tableName
               */
              var fieldName: String = pair.getSecond()
              var fieldInAttr = isFieldInAttr(impl, goToTable, fieldName)
              var fieldIsID = fieldName.equalsIgnoreCase(id)
              var isFKey = isForeignKey(dbScheme, goToTable, fieldName)
              //              if (!fieldInAttr && fieldIsID) { // like: customerID is primary key of PreferredCustomer, but not in attr
              //                // need to find the value of cutsomerID from Cutsomer Table
              //              }
              //              if (fieldInAttr || fieldIsID) {
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
                  var foreignClass: String = getClassByAttr(impl, fieldName)
                  var fieldValue: String = getForeignValue(allInstances, foreignClass, fieldName, impl.getTypeMap())
                  //                  field_part += "`" + fieldName + "`,"
                  field_part += fieldName + ","
                  value_part += fieldValue + ","
                }
              }

              if (fieldInAttr || fieldIsID) {
                var fieldValue = getFieldValue(fieldValuePairs, fieldName, impl.getTypeMap())
                //                field_part += "`" + fieldName + "`,"
                field_part += fieldName + ","
                value_part += fieldValue + ","
              } else if (fieldName.equalsIgnoreCase("DType")) {
                var fieldValue = "'" + className + "'"
                //                field_part += "`" + fieldName + "`,"
                //                var fieldValue = className
                field_part += fieldName + ","
                value_part += fieldValue + ","
              } else if (isFKey) {
                // the fieldName is a foreign key
                // find the primary class of this foreign key name
                // it is an object signature, not an association
                // NOTICE: there is no need to consider association, since association has its own instances
                var primaryClass = getPrimaryClassById(impl, fieldName)
                // the next step is to get the primary key value of primaryClass
                var pKeyValue = getForeignKeyValue(allInstances, primaryClass, fieldName)
                //                field_part += "`" + fieldName + "`,"
                field_part += fieldName + ","
                value_part += pKeyValue + ","
              }
            }
          } else { // the class is an association
            // find two primary classes by foreign keys
            // find value of the primary key of two primary classes
            for (pair <- allAboutSchema if pair.getFirst().equalsIgnoreCase("fields")) {
              var keyName = pair.getSecond
              var keyValue = getFieldValue(fieldValuePairs, keyName, impl.getTypeMap())
              //              field_part += "`" + keyName + "`,"
              field_part += keyName + ","
              value_part += keyValue + ","
            }
          }

          field_part = field_part.substring(0, field_part.length() - 1)
          value_part = value_part.substring(0, value_part.length() - 1)
          //          var stmt: String = "INSERT INTO `" + goToTable + "` (" + field_part + ") VALUES (" + value_part + ");"
          var stmt: String = "INSERT INTO " + goToTable + " (" + field_part + ") VALUES (" + value_part + ");"
          //          stmt += "FLUSH TABLES;";
          // add statments
          if (!dataSchemaHasInsertStatement(allInsertStmts, goToTable, Integer.valueOf(id_value))) {
            addInsertStmtIntoDataSchema(allInsertStmts, goToTable, stmt, Integer.valueOf(id_value));
          }
        }
      }
    }
    var sQueries: SpecializedQuery = new SpecializedQuery()
    sQueries.setInsertStmtsInOneObject(allInsertStmts)
    return sQueries
  }

  def getForeignValue(instances: HashMap[String, HashMap[String, ArrayList[CodeNamePair]]], fClass: String, attr: String, types: HashMap[String, String]): String = {
    var value: String = ""
    var instancesIt = instances.entrySet().iterator()
    while (instancesIt.hasNext()) {
      var entry = instancesIt.next()
      var keyName = entry.getKey()
      if (keyName.equalsIgnoreCase(fClass)) {
        var singleInstanceIt = entry.getValue().entrySet().iterator()
        while (singleInstanceIt.hasNext()) {
          for (pair <- singleInstanceIt.next().getValue()) {
            var field = pair.getFirst().split("_")(1)
            if (field.equalsIgnoreCase(attr)) {
              var tmp: String = pair.getSecond();
              // get the type of field, then handle the value of it
              var fieldType = types.get(field)
              value = fieldType match {
                case "Int" =>
                  var intValue = Integer.valueOf(tmp).intValue()
                  var power = scala.math.pow(2, (AppConfig.getIntScopeForTestCases - 1))
                  intValue = intValue + power.intValue() + 1
                  String.valueOf(intValue)
                case "Real" =>
                  var intValue = Integer.valueOf(tmp).intValue()
                  var power = scala.math.pow(2, (AppConfig.getIntScopeForTestCases - 1))
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
    return value
  }

  def getClassByAttr(impl: DBImplementation, attr: String): String = {
    for (sig <- impl.getSigs()) {
      for (sAttr <- sig.getAttrSet if sig.getCategory == 0) {
        if (sAttr.equalsIgnoreCase(attr)) {
          return sig.getSigName
        }
      }
    }
    return null
  }

  def getPTablesByAssociate(impl: DBImplementation, primaryClass: String): ArrayList[String] = {
    var tables: ArrayList[String] = new ArrayList[String]
    for (sig <- impl.getSigs()) {
      if (sig.getSigName().equalsIgnoreCase(primaryClass) && sig.getCategory() == 1) { // check if sig is an associate
        tables.add(sig.getSrc())
        tables.add(sig.getDst())
      }
    }
    return tables
  }

  def getPKeysOfAssociate(allAboutClass: ArrayList[CodeNamePair]): ArrayList[String] = {
    var keys: ArrayList[String] = new ArrayList[String]
    for (pair <- allAboutClass) {
      if (pair.getFirst().equalsIgnoreCase("primarykey")) {
        keys.add(pair.getSecond())
      }
    }
    return keys;
  }

  def isClassAssociate(impl: DBImplementation, primaryClass: String): Boolean = {
    return impl.getDataProvider().isClassAssociate(primaryClass);
  }

  def getPrimaryClassById(impl: DBImplementation, field: String): String = {
    for (pair <- impl.getReverseIDs) {
      if (pair.getFirst.equalsIgnoreCase(field)) {
        // iterate attrSet
        // check ID is in attr
        for (attr <- impl.getDataProvider().getAttrByTableName(pair.getSecond)) {
          if (attr.equalsIgnoreCase(field)) {
            return pair.getSecond
          }
        }
      }
    }
    return null
  }

  def isFieldInAttr(impl: DBImplementation, mClass: String, attr: String): Boolean = {
    var sigs = impl.getSigs()
    for (sig <- sigs) {
      for (sAttr <- sig.getAttrSet if (sig.getCategory == 0 && sig.getSigName.equalsIgnoreCase(mClass)))
        if (sAttr.equalsIgnoreCase(attr)) {
          return true
        }
    }
    return false
  }

  // return null if not found
  def getTablesByPrimaryKey1(pairs: ArrayList[CodeNamePair], id: String): String = {
    for (p: CodeNamePair <- pairs) {
      if (p.getSecond.equalsIgnoreCase(id)) {
        return p.getFirst
      }
    }
    // default null
    null
  }

  def addInsertStmtIntoDataSchema(allInserts: HashMap[String, HashMap[Integer, String]],
                                  goToTable: String, stmt: String, idValue: Integer) = {
    var contains: Boolean = allInserts.containsKey(goToTable)
    if (!contains) {
      allInserts.put(goToTable, new HashMap[Integer, String])
    }
    allInserts.get(goToTable).put(idValue, stmt)
  }

  def dataSchemaHasInsertStatement(allInserts: HashMap[String, HashMap[Integer, String]],
                                   goToTable: String, idValue: Integer): Boolean = {
    if (allInserts.containsKey(goToTable)) {
      if (allInserts.get(goToTable).containsKey(idValue)) {
        return true
      }
    }
    false
  }

  def getAssByKey(scheme: HashMap[String, ArrayList[CodeNamePair]],
                  pTable: String, fTable: String): HashMap[String, CodeNamePair] = {
    var ass_map: HashMap[String, CodeNamePair] = new HashMap[String, CodeNamePair]()
    var src: String = "";
    var dst: String = "";
    var ass: String = "";

    var schemeIt = scheme.iterator
    while (schemeIt.hasNext) {
      var table = schemeIt.next
      var fields = table._2
      for (pair <- fields) {
        if (pair.getFirst().equalsIgnoreCase("src")) {
          if (pair.getSecond().equalsIgnoreCase(pTable)) {
            src = pTable;
          }
          if (pair.getSecond().equalsIgnoreCase(fTable)) {
            src = fTable;
          }
        }
        if (pair.getFirst().equalsIgnoreCase("dst")) {
          if (pair.getSecond().equalsIgnoreCase(pTable)) {
            dst = pTable;
          }
          if (pair.getSecond().equalsIgnoreCase(fTable)) {
            dst = fTable;
          }
        }
      }
      if (src.length() > 0 && dst.length() > 0) {
        ass = table._1;
        var pair: CodeNamePair = new CodeNamePair(src, dst)
        ass_map.put(ass, pair);
        return ass_map
      }
    }
    null
  }

  // get table name by the primary key
  // we need to filter out the association table by check if the primary key is foreign key at the same time
  def getTablesByPrimaryKey(pKeys: ArrayList[CodeNamePair], primaryKey: String): ArrayList[String] = {
    var tables: ArrayList[String] = new ArrayList[String]();
    for (pair <- pKeys) {
      if (pair.getSecond().equalsIgnoreCase(primaryKey)) {
        tables.add(pair.getFirst());
      }
    }
    return tables
  }

  def getForeignKeyValue(instances: HashMap[String, HashMap[String, ArrayList[CodeNamePair]]], primaryClass: String, pKey: String): String = {
    var instancesIt = instances.entrySet().iterator()
    while (instancesIt.hasNext()) {
      var entry = instancesIt.next()
      var keyName = entry.getKey()
      if (keyName.equalsIgnoreCase(primaryClass)) {
        var singleInstanceIt = entry.getValue().entrySet().iterator()
        while (singleInstanceIt.hasNext()) {
          for (pair <- singleInstanceIt.next().getValue()) {
            if (pair.getFirst().split("_")(1).equalsIgnoreCase(pKey)) {
              var intValue: Integer = Integer.valueOf(pair.getSecond()).intValue();
              var power = scala.math.pow(2, (AppConfig.getIntScopeForTestCases - 1))
              intValue = intValue + power.intValue() + 1;
              return String.valueOf(intValue)
            }
          }
        }
      }
    }
    return null
  }

  def getForeignKeyValue1(instance: HashMap[String, ArrayList[CodeNamePair]],
                          keyValue: String, srcDst: String, srcDst1: String): String = {
    var value: String = null
    var instanceIt = instance.iterator
    while (instanceIt.hasNext) {
      var entryValue = instanceIt.next._2
      for (pair <- entryValue) {
        if (pair.getFirst().split("_")(1).equalsIgnoreCase(srcDst1)) {
          var intValue: Integer = Integer.valueOf(pair.getSecond()).intValue();
          var power = scala.math.pow(2, (AppConfig.getIntScopeForTestCases - 1))
          intValue = intValue + power.intValue() + 1;
          return String.valueOf(intValue)
        }
      }
    }
    return value
  }

  def isForeignKey(scheme: HashMap[String, ArrayList[CodeNamePair]], table: String, field: String): Boolean = {
    for (pair <- scheme.get(table)) {
      if (pair.getFirst().equalsIgnoreCase("foreignKey")) {
        if (pair.getSecond().equalsIgnoreCase(field)) {
          return true
        }
      }
    }
    return false
  }

  def getFieldValue(fieldValues: ArrayList[CodeNamePair], field: String, types: HashMap[String, String]): String = {
    var value: String = null
    for (pair <- fieldValues) {
      if (pair.getFirst().split("_")(1).equalsIgnoreCase(field)) {
        var tmp: String = pair.getSecond();
        // get the type of field, then handle the value of it
        var fieldType = types.get(field)

        value = fieldType match {
          case "Int" =>
            var intValue = Integer.valueOf(tmp).intValue()
            var power = scala.math.pow(2, (AppConfig.getIntScopeForTestCases - 1))
            intValue = intValue + power.intValue() + 1
            String.valueOf(intValue)
          case "Real" =>
            var intValue = Integer.valueOf(tmp).intValue()
            var power = scala.math.pow(2, (AppConfig.getIntScopeForTestCases - 1))
            intValue = intValue + power.intValue() + 1
            String.valueOf(intValue)
          //case "Real" =>
          case "Bool" => "0"
          //var tmpValue = -1
          //                         if(tmp.equalsIgnoreCase("True"))
          //                            tmpValue = 1
          //                         else tmpValue = 0
          //                         String.valueOf(tmpValue)
          case "string" => "'" + tmp + "'"
          case _ => tmp
        }
        return value
        //        if (isNumeric(tmp)) {
        //          var intValue = Integer.valueOf(tmp).intValue()
        //          var power = scala.math.pow(2, (AppConfig.getIntScopeForTestCases - 1))
        //          intValue = intValue + power.intValue() + 1
        //          value = String.valueOf(intValue)
        //          return value
        //        } else {
        //          value = "'" + tmp + "'"
        //          return value
        //        }
      }
    }
    return value
  }

  def isNumeric(str: String): Boolean = {
    var formatter: NumberFormat = NumberFormat.getInstance()
    var pos: ParsePosition = new ParsePosition(0)
    formatter.parse(str, pos);
    return str.length() == pos.getIndex()
  }

  def getPrimaryKeyByTableName(dbScheme: HashMap[String, ArrayList[CodeNamePair]], tableName: String): String = {
    var table: ArrayList[CodeNamePair] = dbScheme.get(tableName);
    //    var pair: CodeNamePair = null
    for (pair <- table) {
      if (pair.getFirst().equalsIgnoreCase("primaryKey")) {
        return pair.getSecond()
      }
    }
    return null
  }

  // looks up reverse t_associate data structure to find a target table for each object element, e.g. a class instance or an association
  def getTableNameByClassName(reverseTAss: ArrayList[CodeNamePair], className: String): String = {
    var elem: CodeNamePair = null
    for (elem <- reverseTAss) {
      if (elem.getFirst().equalsIgnoreCase(className)) {
        return elem.getSecond()
      }
    }
    return null
  }

  def isAssociation(sigs: ArrayList[Sig], element: String): Boolean = {
    for (sig <- sigs) {
      if (sig.getCategory() == 1 && sig.getSigName().equalsIgnoreCase(element)) {
        return true;
      }
    }
    false
  }

  def getParent(sigs: ArrayList[Sig], element: String): String = {
    for (sig <- sigs) {
      if (sig.getCategory() == 0) {
        if (sig.isHasParent()) {
          return sig.getParent()
        }
      }
    }
    null
  }

  def isPrimaryKeys(pKeys: ArrayList[CodeNamePair], table: String, field: String): Boolean = {
    for (s <- pKeys) {
      if (s.getFirst().equalsIgnoreCase(table) && s.getSecond().equalsIgnoreCase(field)) {
        return true;
      }
    }
    return false;
  }

  def specializeSelectQuery(absq: AbstractQuery, impl: DBImplementation, ins: HashMap[String, HashMap[String, ArrayList[CodeNamePair]]]): SpecializedQuery = {
    var selectPart = ""
    var fromPart = ""
    var wherePart = ""
    var allAboutOMClass: ArrayList[CodeNamePair] = new ArrayList[CodeNamePair](1)
    var allSelectStmts: HashMap[String, HashMap[Integer, String]] = new HashMap[String, HashMap[Integer, String]](1)

    var instance = new HashMap[String, HashMap[String, ArrayList[CodeNamePair]]](1)

    if (absq != null) {
      instance = absq.getOodm().parseDocument()
    } else {
      instance = ins
    }

    var instanceIt = instance.iterator
    while (instanceIt.hasNext) {
      var instanceEntry = instanceIt.next
      var element = instanceEntry._1
      var instance = instanceEntry._2
      var isAss = isAssociation(impl.getSigs, element)
      if (!isAss) {
        var instanceIt = instance.iterator
        while (instanceIt.hasNext) {
          var singleInstance = instanceIt.next
          var fieldValuePairs = singleInstance._2

          selectPart = "SELECT ";
          fromPart = " FROM ";
          wherePart = " WHERE ";

          var dbScheme = impl.getDataSchemas
          var goToTable = getTableNameByClassName(impl.getReverseTAssociate, element)

          var id: String = getPrimaryKeyByTableName(dbScheme, goToTable)
          var id_value: Integer = getFieldValue(fieldValuePairs, id, impl.getTypeMap()).toInt

          var parent = getParent(impl.getSigs, element)
          if (parent == null) { // element is a root class
            var allAboutOMClass: ArrayList[CodeNamePair] = dbScheme.get(goToTable)
            //            fromPart += "`" + element + "`"
            fromPart += element
            for (pair <- allAboutOMClass if pair.getFirst.equalsIgnoreCase("fields")) {
              var fieldName = pair.getSecond()
              //              selectPart += "`" + element + "`.`" + fieldName + "`,"
              selectPart += element + "." + fieldName + ","
              if (isPrimaryKeys(impl.getPrimaryKeys, element, fieldName)) {
                val value = getFieldValue(fieldValuePairs, fieldName, impl.getTypeMap())
                //                wherePart += "`" + element + "`.`" + fieldName + "`=" + value + " AND "
                wherePart += element + "." + fieldName + "=" + value + " AND "
              }
            }
          } else if (!goToTable.equalsIgnoreCase(element)) { // class C is mapped to the same table as its super class

          } else if (goToTable.equalsIgnoreCase(element)) { // class C is mapped to its own table
            //            fromPart += "`" + goToTable + "`";
            fromPart += goToTable;
            var allAboutOMClass: ArrayList[CodeNamePair] = dbScheme.get(goToTable)
            for (pair <- allAboutOMClass if pair.getFirst().equalsIgnoreCase("fields")) {
              var fieldName = pair.getSecond()
              //              selectPart += "`" + element + "`.`" + fieldName + "`,";
              selectPart += element + "." + fieldName + ",";
              if (isPrimaryKeys(impl.getPrimaryKeys, element, fieldName)) {
                val value = getFieldValue(fieldValuePairs, fieldName, impl.getTypeMap())
                //                wherePart += "`" + element + "`.`" + fieldName + "`=" + value + " AND ";
                wherePart += element + "." + fieldName + "=" + value + " AND ";
              }
            }
          }
          selectPart = selectPart.substring(0, selectPart.length() - 1);
          wherePart = wherePart.substring(0, wherePart.length() - 5);
          var stmt = selectPart + fromPart + wherePart + ";";
          if (!stmt.substring(0, 11).equalsIgnoreCase("select from")) {
            //            stmt += "RESET QUERY CACHE;";
            if (!dataSchemaHasSelectStatement(allSelectStmts, goToTable, id_value, stmt)) {
              addSelectStmtIntoDataSchema(allSelectStmts, goToTable, id_value, stmt)
            }
          }
        }
      }
    }
    var sq = new SpecializedQuery()
    sq.setSelectStmtsInOneObject(allSelectStmts)
    return sq
  }

  def dataSchemaHasSelectStatement(stmts: HashMap[String, HashMap[Integer, String]], tableName: String, idValue: Integer, stmt: String): Boolean = {
    if (stmts.containsKey(tableName)) {
      if (stmts.get(tableName).containsKey(idValue)) {
        return true
      }
    }
    return false
  }

  def addSelectStmtIntoDataSchema(allStmts: HashMap[String, HashMap[Integer, String]],
                                  tableName: String, idValue: Integer, stmt: String) = {
    if (!allStmts.containsKey(tableName)) {
      allStmts.put(tableName, new HashMap[Integer, String]())
    }
    allStmts.get(tableName).put(idValue, stmt)
  }

  def genObjSpec(fSpec: FormalSpecificationType): ObjectSpec = {
    if (isDebugOn) {
      println("This is myLFunction function")
    }
    /**
     * construct object specification name from FormalSpecification
     * manually set intScopt as 6 (task)
     * new AlloyOMToAllotDM to get sigs
     * new ORMParser to get
     */
    var fSpecPath = fSpec.asInstanceOf[DBFormalSpecification].getSpec
    var objSpecPath = fSpecPath.substring(0, fSpecPath.length() - 4) + "_dm.als"

    var aotad: AlloyOMToAlloyDM = new AlloyOMToAlloyDM()
    // by calling run, (legacy) Object Specification will be created
    aotad.run(fSpecPath, objSpecPath, AppConfig.getIntScopeForTestCases)

    var objSpec = new ObjectSpec()

    var dbDSpec = fSpec.asInstanceOf[DBFormalSpecification]
    objSpec.setIds(dbDSpec.getIds)
    objSpec.setAssociations(dbDSpec.getAssociations)
    objSpec.setTypeList(dbDSpec.getTypeMap())
    objSpec.setSigs(dbDSpec.getSigs())

    objSpec.setSpecPath(objSpecPath)
    objSpec
  }

  def myTFunction(fAB: FormalAbstractMeasurementFunctionSet): (List[ImplementationType] => List[FormalConcreteMeasurementFunctionSet]) = {
    if (isDebugOn) {
      println("This is myTFunction function")
    }

    def returnFunction(implList: List[ImplementationType]): List[FormalConcreteMeasurementFunctionSet] = {
      // convert between List in extracted code and ArrayList in Java
      var impls: ArrayList[DBImplementation] = new ArrayList[DBImplementation]()

      var defaultValue: ImplementationType = Option.empty[ImplementationType].orNull //= new ImplementationType
      var implHd: ImplementationType = hd[ImplementationType](defaultValue)(implList)
      var implTl = tl[ImplementationType](implList)

      while (implHd != defaultValue) {
        impls.add(implHd.asInstanceOf[DBImplementation])
        var tmp = hd[ImplementationType](defaultValue)(implTl)
        if (tmp != Nil[ImplementationType]()) {
          implHd = tmp.asInstanceOf[DBImplementation]
          implTl = tl[ImplementationType](implTl)
        }
        implHd = tmp;
      }

      // for each implementation, get concrete MF from abstract MD
      // return ArrayList
      var concreteMFSet = getConcreteMeasurementFunctionSets(fAB.asInstanceOf[DBFormalAbstractMeasurementFunctionSet], impls)

      // new empty list
      var returnValue: List[FormalConcreteMeasurementFunctionSet] = Nil[FormalConcreteMeasurementFunctionSet]()
      var cMFSIt = concreteMFSet.iterator()
      while (cMFSIt.hasNext()) {
        var tmp = cMFSIt.next()
        returnValue = Cons[FormalConcreteMeasurementFunctionSet](tmp.asInstanceOf[FormalConcreteMeasurementFunctionSet], returnValue)
      }
      // return from inner function
      return returnValue
    }
    // return outter function
    return returnFunction
  }

  def mySFunction(spec: SpecificationType): FormalSpecificationType = {
    if (isDebugOn) {
      println("This is mySFunction function")
    }
    var dbfs = new DBFormalSpecification(spec.asInstanceOf[DBSpecification].getSpecFile)
    // parse spec file and fill in all members
    // Chong: check how to define constructor in Scala, and call parseSepc() in consctructor
    dbfs.parseSpec()
    dbfs
  }

  def myIFunctionHelper(fciList: List[FormalImplementationType]): List[ImplementationType] = {
    var implList: List[ImplementationType] = Nil[ImplementationType]()

    var defaultValue = new DBFormalImplementation()

    var tmp = hd[FormalImplementationType](defaultValue)(fciList)
    var tail = tl[FormalImplementationType](fciList)
    while (tmp != defaultValue) {
      var dbi: ImplementationType = myIFunction(tmp)
      implList = Cons[ImplementationType](dbi.asInstanceOf[DBImplementation], implList)
      tmp = hd[FormalImplementationType](defaultValue)(tail)
      tail = tl[FormalImplementationType](tail)
    }
    implList
  }

  def myIFunction(fImp: FormalImplementationType): ImplementationType = {
    if (isDebugOn) {
      //      println("This is myIFunction function")
    }
    /**
     * compute FormalImplementation schema name
     * sigs here is all signatures in FormalSpecification (alloyOM), which already be set by lFunction
     * set all needed information for test cases generation here, initialize the global variable SolveAlloyDM
     */
    var fImpFileName = fImp.asInstanceOf[DBFormalImplementation].getImplementation
    var impFileName = fImpFileName.substring(0, fImpFileName.length() - 4) + ".sql"

    var parser = new ORMParser(fImpFileName, impFileName, fImp.asInstanceOf[DBFormalImplementation].getSigs)
    parser.createSchemas();
    /**
     * Need to set all needed information for test cases generation here
     */

    var dbFImpl: DBFormalImplementation = fImp.asInstanceOf[DBFormalImplementation]

    var impl = new DBImplementation(impFileName)
    impl.setAllFields(parser.getallFields())
    impl.setPrimaryKeys(parser.getPrimaryKeys())
    impl.setReverseTAssociate(parser.getReverseTAssociate())
    impl.setFields(parser.getFields())
    impl.setFieldsTable(parser.getFieldsTable())
    impl.setForeignKeys(parser.getForeignKey())
    impl.setDataProvider(parser.getDataProvider())
    impl.setAssociations(parser.getAssociations())
    impl.setReverseIDs(parser.getReverseIds())

    impl.setSigs(dbFImpl.getSigs)
    impl.setIds(dbFImpl.getIds)
    impl.setAssociationsForCreateSchemas(dbFImpl.getAssociationsForCreateSchemas)
    impl.setTypeMap(dbFImpl.getTypeMap)

    impl
  }

  def myBFunctionHelper(fcbList: List[FormalConcreteMeasurementFunctionSet]): List[MeasurementFunctionSetType] = {
    // iterate whole list Concrete Measurement Function
    // define a default value to call hd()
    var mfSetList: List[MeasurementFunctionSetType] = Nil[MeasurementFunctionSetType]()
    var defaultValue = Nil[FormalConcreteMeasurementFunctionSet]()
    var fcfHead = hd[FormalConcreteMeasurementFunctionSet](defaultValue)(fcbList)
    var fcfTail = tl[FormalConcreteMeasurementFunctionSet](fcbList)

    while (fcfHead != defaultValue) {
      var result = myBFunction(fcfHead)
      mfSetList = Cons[MeasurementFunctionSetType](result, mfSetList)
      fcfHead = hd[FormalConcreteMeasurementFunctionSet](defaultValue)(fcfTail)
      fcfTail = tl[FormalConcreteMeasurementFunctionSet](fcfTail)
    }
    mfSetList
  }

  // BFunction here is an identity function
  def myBFunction(fCB: FormalConcreteMeasurementFunctionSet): MeasurementFunctionSetType = {
    var castedfCB = fCB.asInstanceOf[DBFormalConcreteMeasurementFunctionSet]
    var tLoads = castedfCB.getCtmf().getLoads()
    var sLoads = castedfCB.getCsmf().getLoads()

    var dbConTMF = new DBConcreteTimeMeasurementFunction(tLoads)
    var dbConSMF = new DBConcreteSpaceMeasurementFunction(sLoads)

    var dbConMF = new DBConcreteMeasurementFunctionSet(dbConTMF, dbConSMF)
    dbConMF.setImpl(castedfCB.getImpl())

    dbConMF
  }

  var myTrademaker: Trademaker = new Build_Trademaker(
    myTradespace,
    myParetoFront,
    myCFunction,
    myAFunction,
    myLFunction,
    myTFunction,
    mySFunction,
    myIFunction,
    myBFunction)
}
