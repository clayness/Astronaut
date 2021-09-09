package edu.virginia.cs

import com.typesafe.config._

object AppConfig {
  private final val params: Config = ConfigFactory.load("application")

  private final val debug: Boolean = params.getBoolean("dev.debug")
  private final val mysqlUser: String = params.getString("mysql.user")
  private final val mysqlPassword: String = params.getString("mysql.password")
  private final val intScopeForImpl: Integer = params.getInt("alloy.intScopeForImpl")
  private final val intScopeForTestCases: Integer = params.getInt("alloy.intScopeForTestCases")
  private final val maxSolForImpl: Integer = params.getInt("alloy.maxSolForImpl")
  private final val maxSolForTest: Integer = params.getInt("alloy.maxSolForTest")
  private final val A4ReportSymmetry: Integer = params.getInt("alloy.A4Report.symmetry")
  private final val A4ReportSkolemDepth: Integer = params.getInt("alloy.A4Report.skolemDepth")
  private final val isRandom: Integer = params.getInt("alloy.tlGenerator")
  private final val randomRange: Integer = params.getInt("alloy.randomRange")
  private final val subRange: Integer = params.getInt("alloy.subRange")
  private final var specList: java.util.List[String] = params.getStringList("app.specs")
  private final val solver = params.getString("app.solver")
  private final val storeAllSolutions: Boolean = params.getBoolean("app.storeAllSolution")
  private final val hdfsURL = params.getString("hadoop.server")
  private final val hdfsFile = params.getString("hadoop.file")
  private final val sparkMaster = params.getString("spark.master")
  private final val resultFile = params.getString("resultFile")
  private final val sparkSlaves: java.util.List[String] = params.getStringList("spark.slaves")
  private final var testDB: String = params.getString("app.testDB")
  private final val postgresUser: String = params.getString("postgres.user")
  private final val postgresPassword: String = params.getString("postgres.password")
  private final var icse2022SolutionFolder: String = if (params.hasPath("icse2022.solutionFolder")) params.getString("icse2022.solutionFolder") else ""

  def getPostgresUser: String = {
    this.postgresUser
  }

  def getPostgresPassword: String = {
    this.postgresPassword
  }

  def getTestDB: String = {
    this.testDB
  }

  def setTestDB(testDB: String): Unit = {
    this.testDB = testDB
  }

  def getSolutionFolder: String = {
    this.icse2022SolutionFolder
  }

  def setSolutionFolder(folder: String): Unit = {
    this.icse2022SolutionFolder = folder
  }

  def setSpecList(list: java.util.List[String]): Unit = {
    this.specList = list
  }

  def getResultFile: String = {
    this.resultFile
  }

  def getHdfsURL: String = {
    this.hdfsURL
  }

  def getHdfsFile: String = {
    this.hdfsFile
  }

  def getSparkMaster: String = {
    this.sparkMaster
  }

  def getSparkSlaves: java.util.List[String] = {
    this.sparkSlaves
  }

  def getStoreAllSolutions: Boolean = {
    this.storeAllSolutions
  }

  def getSolver: String = {
    this.solver
  }

  def getSpecs: java.util.List[String] = {
    this.specList
  }

  def getDebug: Boolean = {
    this.debug
  }

  def getMySQLUser: String = {
    this.mysqlUser
  }

  def getMysqlPassword: String = {
    this.mysqlPassword
  }

  def getIntScopeForImpl: Integer = {
    this.intScopeForImpl
  }

  def getIntScopeForTestCases: Integer = {
    this.intScopeForTestCases
  }

  def getMaxSolForImpl: Integer = {
    this.maxSolForImpl
  }

  def getMaxSolForTest: Integer = {
    this.maxSolForTest
  }

  def getA4ReportSymmetry: Integer = {
    this.A4ReportSymmetry
  }

  def getA4ReportSkolemDepth: Integer = {
    this.A4ReportSkolemDepth
  }

  def getIsRandom: Integer = {
    this.isRandom
  }

  def getRandomRange: Integer = {
    this.randomRange
  }

  def getSubRange: Integer = {
    this.subRange
  }
}
