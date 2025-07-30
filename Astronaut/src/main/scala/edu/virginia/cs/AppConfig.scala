package edu.virginia.cs

import com.typesafe.config._

import java.util
import scala.jdk.CollectionConverters._

object AppConfig {
  private final val params: Config = ConfigFactory.load("application")

  private final val debug: Boolean = params.getBoolean("dev.debug")
  private final val mysqlUser: String = params.getString("mysql.user")
  private final val mysqlPassword: String = params.getString("mysql.password")
  private final val intScopeForTestCases: Integer = params.getInt("alloy.intScopeForTestCases")
  private final val maxSolForImpl: Integer = params.getInt("alloy.maxSolForImpl")
  private final val maxSolForTest: Integer = params.getInt("alloy.maxSolForTest")
  private final val A4ReportSymmetry: Integer = params.getInt("alloy.A4Report.symmetry")
  private final val A4ReportSkolemDepth: Integer = params.getInt("alloy.A4Report.skolemDepth")
  private final val isRandom: Integer = params.getInt("alloy.tlGenerator")
  private final val randomRange: Integer = params.getInt("alloy.randomRange")
  private final val subRange: Integer = params.getInt("alloy.subRange")
  private final val storeAllSolutions: Boolean = params.getBoolean("app.storeAllSolution")
  private final val resultFile = params.getString("resultFile")
  private final val sparkSlaves: List[String] = params.getStringList("spark.slaves").asScala.toList
  private final val postgresUser: String = params.getString("postgres.user")
  private final val postgresPassword: String = params.getString("postgres.password")
  private final val icse2022SolutionFolder: String = if (params.hasPath("icse2022.solutionFolder")) params.getString("icse2022.solutionFolder") else ""
  private final var specList: List[String] = params.getStringList("app.specs").asScala.toList
  private final var testDB: String = params.getString("app.testDB")

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

  def setSpecList(list: List[String]): Unit = {
    this.specList = list
  }

  def getResultFile: String = {
    this.resultFile
  }

  def getSparkSlaves: util.List[String] = {
    this.sparkSlaves.asJava
  }

  def getStoreAllSolutions: Boolean = {
    this.storeAllSolutions
  }

  def getSpecs: List[String] = {
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

  //  def getSpecificationPath(): String = {
  //    this.specificationPath
  //  }

  //  def getImplsPath(): String = {
  //    this.implsPath
  //  }

  //  def getTestCasesPath(): String = {
  //    this.testCasesPath
  //  }

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
}
