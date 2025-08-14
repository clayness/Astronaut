package wikalloy

import com.typesafe.config._

object WikalloyConfig {
  private final val params: Config = ConfigFactory.load("application")

  def getNumInstances: Int      = params.getInt("wikalloy.numInstances")
  def getNumLoads: Int          = params.getInt("wikalloy.numLoads")
  def getNumQueries: Int        = params.getInt("wikalloy.numQueries")
  def getOutputPath: String     = params.getString("wikalloy.outputPath")
  def getMySQLUri: String       = params.getString("mysql.uri")
  def getMySQLUsername: String  = params.getString("mysql.username")
  def getMySQLPassword: String  = params.getString("mysql.password")
  def getSolutionMax:  Int      = params.getInt("alloy.solutionMax")
  def getSolutionFolder: String = params.getString("alloy.solutionFolderPath")
}
