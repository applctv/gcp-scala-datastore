name := "datastore-scala-wrapper"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.google.cloud" % "google-cloud" % "0.8.0"
)