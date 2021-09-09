import Dependencies._

ThisBuild / scalaVersion     := "2.11.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "Astronaut",
    assembly / mainClass := Some("edu.virginia.cs.Main"),
    assembly / assemblyJarName := "astronaut.jar",
    libraryDependencies ++= Seq(jdom, mysql, typesafe, spark)
  )
