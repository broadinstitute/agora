import sbtassembly.MergeStrategy
import sbtrelease.ReleasePlugin._

name := "Agora"

organization := "org.broadinstitute"

scalaVersion := "2.11.7"

val sprayV = "1.3.2"

val artifactory = "https://artifactory.broadinstitute.org/artifactory/"

resolvers += "artifactory-releases" at artifactory + "libs-release"

resolvers += "artifactory-snapshots" at artifactory + "libs-snapshot"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.github.swagger-spray" %% "swagger-spray" % "0.8.0",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2",
  "com.google.api-client" % "google-api-client" % "1.20.0" excludeAll ExclusionRule(organization = "com.google.guava"),
  "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev53-1.20.0" excludeAll ExclusionRule(organization = "com.google.guava"),
  "com.h2database" % "h2" % "1.3.175",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.11",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "com.zaxxer" % "HikariCP" % "2.3.9",
  "io.spray" %% "spray-can" % sprayV,
  "io.spray" %% "spray-client" % sprayV,
  "io.spray" %% "spray-json" % "1.3.1", // NB: Not at sprayV. 1.3.2 does not exist.
  "io.spray" %% "spray-routing" % sprayV,
  "mysql" % "mysql-connector-java" % "5.1.35",
  "org.broadinstitute" %% "wdl4s" % "0.11",
  "org.broadinstitute.dsde.vault" %% "vault-common" % "0.1-15-bf74315",
  "org.mongodb" %% "casbah" % "2.8.2",
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "org.webjars" % "swagger-ui"  % "2.2.5",
  //---------- Test libraries -------------------//
  "io.spray" %% "spray-testkit" % sprayV % Test,
  "org.scalatest" %% "scalatest" % "2.2.4" % Test
)

//These can be overrided with system properties:
// i.e: sbt -Dflyway.url=jdbc:mysql://DB_HOST:DB_PORT/DB_NAME -Dflyway.user=root -Dflyway.password=abc123
flywayUrl := "jdbc:h2:file:local"

flywayUser := "root"

flywayCleanDisabled := true

releaseSettings

shellPrompt := { state => "%s| %s> ".format(GitCommand.prompt.apply(state), version.value) }

// We need to fork the tests and provide the correct config so that users do not accidentally
// provide a config that points to a real database.
javaOptions in Test := Seq("-Dconfig.file=src/test/resources/reference.conf")
fork in Test := true

parallelExecution in Test := false

//assembly settings
test in assembly := {}

assemblyJarName in assembly := "agora-" + version.value + ".jar"

logLevel in assembly := Level.Info

assemblyMergeStrategy in assembly := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs@_*) =>
    xs map {
      _.toLowerCase
    } match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps@(x :: strings) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: strings =>
        MergeStrategy.discard
      case "spring.tooling" :: strings =>
        MergeStrategy.discard
      case "com.google.guava" :: strings =>
        MergeStrategy.discard
      case "services" :: strings =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case "asm-license.txt" | "overview.html" =>
    MergeStrategy.discard
  case "cobertura.properties" =>
    MergeStrategy.discard
  case "logback.xml" =>
    MergeStrategy.first
  case _ => MergeStrategy.deduplicate
}

Revolver.settings.settings
Revolver.enableDebugging(port = 5051, suspend = false)

// When JAVA_OPTS are specified in the environment, they are usually meant for the application
// itself rather than sbt, but they are not passed by default to the application, which is a forked
// process. This passes them through to the "re-start" command, which is probably what a developer
// would normally expect.
javaOptions in reStart ++= sys.env("JAVA_OPTS").split(" ").toSeq
