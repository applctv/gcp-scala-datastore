name := "datastore-scala-wrapper"

organization := "io.applicative"

version := "1.0-rc13"

scalaVersion := "2.12.8"
crossScalaVersions := Seq("2.11.12", "2.12.8")

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

homepage := Some(url("https://github.com/applctv/gcp-scala-datastore/"))

description := "Scala wrapper for Google Cloud Datastore"

sonatypeCredentialHost := "central.sonatype.com"
publishTo := sonatypePublishToBundle.value
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_central_credentials")

// GPG Signing configuration
usePgpKeyHex("EC0E377F561C86A620619E335B78C65506F9E48D")
pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toCharArray)

publishMavenStyle := true

scmInfo := Some(
  ScmInfo(
    url("https://github.com/applctv/gcp-scala-datastore/"),
    "scm:git:git@github.com:applctv/gcp-scala-datastore.git"
  )
)

developers := List(
  Developer(
    id = "applctv",
    name = "Applicative",
    email = "hello@applicative.io",
    url = url("http://applicative.io")
  ),
  Developer(
    id = "a-panchenko",
    name = "Oleksandr Panchenko",
    email = "o.panchenko@applicative.io",
    url = url("https://github.com/a-panchenko")
  )
)


libraryDependencies ++= {
  val gcdJavaSDKVersion = "1.82.0"
  val specsVersion = "4.6.0"

  Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.google.cloud" % "google-cloud-datastore" % gcdJavaSDKVersion,
    "org.specs2" %% "specs2-core" % specsVersion % "test",
    "org.specs2" %% "specs2-mock" % specsVersion % "test"
  )
}
