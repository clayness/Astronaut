name := "Astronaut"
version := "1.0"
scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  "org.jdom" % "jdom2" % "2.0.6.1",
  "com.mysql" % "mysql-connector-j" % "8.3.0",
  "com.typesafe" % "config" % "1.4.3",
  "org.apache.spark" %% "spark-core" % "4.0.0" % "provided"
)

assembly / assemblyJarName := "astronaut.jar"

assembly / mainClass := Some("edu.virginia.cs.Main")