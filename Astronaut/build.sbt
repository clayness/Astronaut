name := "Astronaut"
version := "1.0"
scalaVersion := "2.13.16"

javacOptions ++= Seq("-source", "17", "-target", "17")

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "org.jdom" % "jdom2" % "2.0.6.1",
  "com.mysql" % "mysql-connector-j" % "9.4.0",
  "com.typesafe" % "config" % "1.4.4",
  "org.apache.spark" %% "spark-core" % "4.0.0" % "provided",
  "org.alloytools.alloy" % "AlloyTools" % "5.1.0" from "https://github.com/AlloyTools/org.alloytools.alloy/releases/download/v5.1.0/org.alloytools.alloy.dist.jar",
  "info.picocli" % "picocli" % "4.7.7",
  "org.scala-lang.modules" % "scala-parallel-collections_2.13" % "1.2.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.25.1",
  "org.apache.logging.log4j" % "log4j-core" % "2.25.1",
  "org.apache.logging.log4j" %% "log4j-api-scala" % "13.1.0"
)

assembly / assemblyJarName := "astronaut.jar"

assembly / mainClass := Some("edu.virginia.cs.Main")

assembly / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}