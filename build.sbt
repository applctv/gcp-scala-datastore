name := "datastore-scala-wrapper"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.google.cloud" % "google-cloud" % "0.8.0",
  "org.specs2" %% "specs2-core" % "3.8.8" % "test",
  "org.specs2" % "specs2-mock_2.11" % "3.8.8" % "test"
//  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)