name := "datastore-scala-wrapper"

organization := "io.applicative"

version := "1.0-rc9"

scalaVersion := "2.12.6"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization := Some("applctv")

bintrayRepository := "gcp-scala-datastore"

bintrayVcsUrl := Some("git@github.com:applctv/gcp-scala-datastore.git")

crossScalaVersions := Seq("2.11.11", scalaVersion.value)

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


libraryDependencies ++= {
  val gcdJavaSDKVersion = "1.36.0"
  val specsVersion = "3.8.8"

  Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.google.cloud" % "google-cloud-datastore" % gcdJavaSDKVersion,
    "org.specs2" %% "specs2-core" % specsVersion % "test",
    "org.specs2" %% "specs2-mock" % specsVersion % "test"
  )
}
