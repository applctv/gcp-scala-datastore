name := "datastore-scala-wrapper"

organization := "io.applicative"

version := "1.0-rc7"

scalaVersion := "2.11.8"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization := Some("applctv")

bintrayRepository := "gcp-scala-datastore"

bintrayVcsUrl := Some("git@github.com:applctv/gcp-scala-datastore.git")

// Publish settings for Maven Central
publishMavenStyle := true
pomExtra := (
    <url>https://github.com/applctv/gcp-scala-datastore/</url>
    <scm>
      <url>git@github.com:applctv/gcp-scala-datastore.git</url>
      <connection>scm:git:git@github.com:applctv/gcp-scala-datastore.git</connection>
    </scm>
    <developers>
      <developer>
        <id>applctv</id>
        <name>Applicative</name>
        <url>http://applicative.io</url>
      </developer>
      <developer>
        <id>a-panchenko</id>
        <name>Oleksandr Panchenko</name>
      </developer>
    </developers>)


libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.google.cloud" % "google-cloud-datastore" % "1.3.1",
  "org.specs2" %% "specs2-core" % "3.8.8" % "test",
  "org.specs2" % "specs2-mock_2.11" % "3.8.8" % "test"
)
