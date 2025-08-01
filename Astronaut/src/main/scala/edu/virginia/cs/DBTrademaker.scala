package edu.virginia.cs

import analysis.{QueryGenerator, SparkAnalyzer}
import edu.virginia.cs.Framework.Types.AbstractQuery.Action
import edu.virginia.cs.Framework.Types._
import edu.virginia.cs.Framework._
import edu.virginia.cs.Synthesizer._
import edu.virginia.cs.Uniq.DeleteUniq

import java.io.{File, PrintWriter}
import java.nio.file.Path
import java.util
import java.util.{Random, UUID}
import scala.jdk.CollectionConverters._
import scala.util.Using

class DBTrademaker extends AstronautFramework {
  // analyze and tradespace are already defined in Tradespace specification
  // we can call "tradespace" function to synthesize implementation and benchmark
  private val myTradespace: Tradespace = Build_Tradespace(synthesizeImplAndFuncFromSpec, analyzeWithSpark)
  var isDebugOn: Boolean = AppConfig.getDebug
  var startTime: Long = 1
  var endTime: Long = 1

  def run(): Unit = {
    // iterate the list of specs from the configuration
    val specs = AppConfig.getSpecs
    for (spec <- specs) {
      // get the name of the specification file
      val leaf = Path.of(spec).getFileName.toString.replaceFirst("\\.[^.]+$", "")
      // synthesize the tradespace for the spec
      val mySpec: DBSpecification = new DBSpecification(spec)
      // invokes the load synthesizer and the Spark analyzer (distributed, may take some time)
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

  private def analyzeWithSpark(list: List[(ImplementationType, MeasurementFunctionSetType)]): List[(ImplementationType, MeasurementResultSetType)] =
    new SparkAnalyzer().analyze(list)

  private def getIDBySigName(sigs: util.List[Sig], sigName: String): String = {
    sigs.asScala.collectFirst {
      case s if s.getSigName.equalsIgnoreCase(sigName) => s.getId
    }.getOrElse("")
  }

  private def synthesizeImplAndFuncFromSpec(spec: SpecificationType)
  : List[(ImplementationType, MeasurementFunctionSetType)] = {
    val fSpec: FormalSpecificationType = createFormalSpec(spec)
    val fImpl: List[FormalImplementationType] = createFormalImpls(fSpec)
    val impls = fImpl.map(myIFunction)

    if (AppConfig.getIsRandom == 0) {
      val fAbsMF: FormalAbstractMeasurementFunctionSet = myLFunction(fSpec)
      val fConMF: List[FormalConcreteMeasurementFunctionSet] = myTFunction(fAbsMF)(impls)
      impls.zip(fConMF.map(myBFunction))
    } else if (AppConfig.getIsRandom == 1) {
      // get concrete measurement function by random generator
      impls.zip(genRandomConcreteMF(impls))
    } else {
      null
    }
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
      val mappingRun: String = new FrontParser(specPath).createMappingRun
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
    // generate two abstract load objects: insert and select
    // create two measurement functions, one for time, one for space
    // wrap them in a FormalAbstractMeasurementFunctionSet
    val absLoads: FormalAbstractLoadSet = generateFormalAbstractLoadSet(fSpec)
    val absTimeMeasurementFunction = new DBFormalAbstractTimeMeasurementFunction(absLoads.getInsLoad, absLoads.getSelLoad)
    new DBFormalAbstractMeasurementFunctionSet(absTimeMeasurementFunction)
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
        for (elem <- insertS.asScala) {
          if (elem._1.equalsIgnoreCase(s)) {
            val tmp = elem._2
            for (stmt <- tmp.asScala) {
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
        for (elem <- selectS.asScala) {
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
    val qg = new QueryGenerator()
    val cq = new ConcreteQuery()
    val a = absq.getAction
    if (a == Action.INSERT) {
      cq.setSq(qg.specializeInsertQuery(absq, impl, null))
    } else {
      cq.setSq(qg.specializeSelectQuery(absq, impl, null))
    }
    cq
  }

  private def genObjSpec(fSpec: FormalSpecificationType): ObjectSpec = {
    val fSpecPath = fSpec.getSpec
    val objSpecPath = fSpecPath.substring(0, fSpecPath.length() - 4) + "_dm.als"

    val aotad: AlloyOMToAlloyDM = new AlloyOMToAlloyDM()
    aotad.run(fSpecPath, objSpecPath, AppConfig.getIntScopeForTestCases)

    val objSpec = new ObjectSpec()

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
    val fImpFileName = fImp.getImplementation
    val impFileName = fImpFileName.substring(0, fImpFileName.length() - 4) + ".sql"

    val parser = new ORMParser(fImpFileName, impFileName, fImp.getSigs)
    parser.createSchemas()

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

    new DBConcreteMeasurementFunctionSet(dbConTMF, dbConSMF)
  }
}
