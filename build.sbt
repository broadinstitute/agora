import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._
import sbtrelease.ReleasePlugin._

name := "Agora"

organization := "org.broadinstitute"

scalaVersion := "2.11.6"

val sprayV = "1.3.2"

val artifactory = "https://artifactory.broadinstitute.org/artifactory/"

resolvers += "artifactory-releases" at artifactory + "libs-release"

resolvers += "artifactory-snapshots" at artifactory + "libs-snapshot"

libraryDependencies ++= Seq(
  "cglib" % "cglib-nodep" % "2.2",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.gettyimages" %% "spray-swagger" % "0.5.0",
  "com.google.api-client" % "google-api-client" % "1.20.0" excludeAll ExclusionRule(organization = "com.google.guava"),
  "com.google.apis" % "google-api-services-storage" % "v1-rev35-1.20.0",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.11",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "io.spray" %% "spray-can" % sprayV,
  "io.spray" %% "spray-client" % sprayV,
  "io.spray" %% "spray-json" % "1.3.1", // NB: Not at sprayV. 1.3.2 does not exist.
  "io.spray" %% "spray-routing" % sprayV,
  "net.ceedubs" %% "ficus" % "1.1.2",
  "org.broadinstitute" %% "cromwell" % "0.1-SNAPSHOT" excludeAll ExclusionRule(organization = "com.gettyimages"),
  "org.broadinstitute.dsde.vault" %% "vault-common" % "0.1-15-bf74315",
  "org.mongodb" %% "casbah" % "2.8.1",
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "org.webjars" % "swagger-ui" % "2.0.24",
  //---------- Test libraries -------------------//
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2" % Test,
  "io.spray" %% "spray-testkit" % sprayV % Test,
  "org.scalatest" %% "scalatest" % "2.2.4" % Test
)

releaseSettings

shellPrompt := { state => "%s| %s> ".format(GitCommand.prompt.apply(state), version.value) }

jarName in assembly := "agora-" + version.value + ".jar"

logLevel in assembly := Level.Info

val customMergeStrategy: String => MergeStrategy = {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps@_*) if (Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last)) =>
    MergeStrategy.rename
  case PathList("META-INF", xs@_*) =>
    xs map {
      _.toLowerCase
    } match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "spring.tooling" :: xs =>
        MergeStrategy.discard
      case "com.google.guava" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case "asm-license.txt" | "overview.html" =>
    MergeStrategy.discard
  case "logback.xml" =>
    MergeStrategy.first
  case _ => MergeStrategy.deduplicate
}

mergeStrategy in assembly := customMergeStrategy

test in assembly := {}

val integrationTest = inputKey[Unit]("Runs the Agora integration tests.")

integrationTest := {
  (testOnly in Test).toTask(" org.broadinstitute.dsde.agora.server.AgoraIntegrationTestSuite").value
}

val unitTest = inputKey[Unit]("Runs the Agora unit tests.")

unitTest := {
  (testOnly in Test).toTask(" org.broadinstitute.dsde.agora.server.AgoraUnitTestSuite").value
}