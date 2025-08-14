package wikalloy

import org.apache.logging.log4j.scala.Logging
import wikalloy.generic.AbstractLoadFactory

import java.io.PrintWriter
import java.nio.file.Path
import scala.util.Using
import scala.util.control.Breaks.{break, breakable}

class WikalloyAnalyzer extends Logging {

  def run(args: Array[String]): Unit = {
    if (args.isEmpty)
      throw new IllegalArgumentException("No specification path(s) provided.")
    // iterate the list of specs
    for (spec <- args) {
      logger.info(s"Starting spec: $spec")
      // get the absolute path to the OODM spec
      val oodmSpecPath = Path.of(spec).toAbsolutePath
      // synthesize the models for the mapping spec
      // solve the OODM spec and create the object model
      val oodmSolutions = new AlloySolutionIterator(oodmSpecPath)
      if (!oodmSolutions.hasNext) {
        throw new UnsupportedOperationException(s"No solutions found for OODM spec: $oodmSpecPath")
      }
      val oodmSolution = oodmSolutions.next()
      val oodm = new ObjectModelFactory(oodmSolution).create
      logger.info("Object model created.")
      // create a set of abstract loads
      val alf = new AbstractLoadFactory(oodmSolution, oodm)
      val als = List.fill(WikalloyConfig.getNumLoads) {
        alf.create(WikalloyConfig.getNumInstances, WikalloyConfig.getNumQueries)
      }
      logger.info(s"Generated ${als.size} abstract loads.")
      // invokes the load synthesizer and the Spark analyzer (distributed, may take some time)
      logger.info("Starting Spark analysis.")
      val cis = getMappingModels(oodmSpecPath).map(p =>
          Path.of("/opt/solutions/").resolve(Path.of(WikalloyConfig.getSolutionFolder).toAbsolutePath.relativize(p)))
        .map(_.toAbsolutePath.toString)
      val evaluatedResults = new SparkAnalyzer().analyze(cis, als, oodm)
      if (evaluatedResults.nonEmpty) {
        // get solution file name, which is like: customerOrderObjectModel_Sol_2.sql
        val outPath = Path.of(WikalloyConfig.getOutputPath).resolve(oodmSpecPath.getFileName.toString + ".csv")

        // iterate the results and print each result to the output file
        Using(new PrintWriter(outPath.toFile)) { pw =>
          pw.printf("%20s,%12s,%12s,%12s,%12s%n",
            "Solution", "CreateTime", "InsertTime", "SelectTime", "StorageSize")
          for (mr <- evaluatedResults) {
            pw.printf("%20s,%12d,%12.2f,%12.2f,%12.2f%n",
              mr.name(),
              mr.createTime(),
              mr.insertTime(),
              mr.selectTime(),
              mr.space()
            )
          }
        }
      }
    }
  }

  private def getMappingModels(oodmSpecPath: Path): List[Path] = {
    var ret = List[Path]()
    val (modelFolderPath, isEmpty) = prepareModelFolder(oodmSpecPath)
    if (isEmpty) {
      val startTime = System.currentTimeMillis()
      // make sure the mapping run file exists
      val mapSpecPath = oodmSpecPath.resolveSibling("map." + oodmSpecPath.getFileName.toString)
      if (!mapSpecPath.toFile.exists()) {
        throw new IllegalArgumentException(s"Mapping run file does not exist: $mapSpecPath")
      }
      // generate the solutions
      val asi = new AlloySolutionIterator(mapSpecPath)
      breakable {
        for (i <- 0 until WikalloyConfig.getSolutionMax) {
          if (!asi.hasNext) {
            break()
          }
          val modelPath = modelFolderPath.resolve(f"MODL_$i%06d.xml").toAbsolutePath
          Using(new PrintWriter(modelPath.toFile)) { pw =>
            asi.next().writeXML(pw, null, null)
            ret = ret :+ modelPath
          }
        }
      }
      // log the time taken to synthesize all the results
      val synthTime = System.currentTimeMillis() - startTime
      println(s"[MODEL FINDING] Synthesis complete: time=$synthTime, spec=$oodmSpecPath")
    } else {
      println(s"[MODEL FINDING] Reading results from folder: $modelFolderPath")
      modelFolderPath.toFile.listFiles.filter(_.getName.endsWith(".xml")).foreach(f => ret = ret :+ f.toPath.toAbsolutePath)
    }
    ret
  }

  private def prepareModelFolder(specPath: Path): (Path, Boolean) = {
    // get the name of the spec without the extension
    var specName = specPath.getFileName.toString
    specName = specName.substring(0, specName.lastIndexOf("."))
    // get the configured solution folder
    var solutionFolder = WikalloyConfig.getSolutionFolder
    if (solutionFolder == null || solutionFolder.trim.isEmpty) {
      throw new IllegalArgumentException("Solution folder is not specified.")
    }
    // make sure the solution path exists
    val solutionPath = Path.of(solutionFolder)
    solutionPath.toFile.mkdirs()
    // check if the model folder exists and contains solutions
    val modelPath = solutionPath.resolve(specName)
    if (modelPath.toFile.exists()) {
      (modelPath, false)
    } else {
      modelPath.toFile.mkdirs()
      (modelPath, true)
    }
  }
}
