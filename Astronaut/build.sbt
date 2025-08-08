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
  "com.google.code.gson" % "gson" % "2.13.1",
  "info.picocli" % "picocli" % "4.7.7"
)

assembly / assemblyJarName := "astronaut.jar"

assembly / mainClass := Some("edu.virginia.cs.Main")