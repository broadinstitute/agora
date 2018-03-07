import sbtassembly.MergeStrategy
import sbtrelease.ReleasePlugin._

name := "Agora"

organization := "org.broadinstitute"

scalaVersion := "2.12.4"

// uncomment to see compile warnings
// scalacOptions := Seq("-unchecked", "-deprecation", "-feature")

lazy val akkaV = "2.5.9"
lazy val akkaHttpV = "10.0.11"

val cromwellVersion = "30-4de204a"

val artifactory = "https://broadinstitute.jfrog.io/broadinstitute/"

resolvers += "artifactory-releases" at artifactory + "libs-release"

resolvers += "artifactory-snapshots" at artifactory + "libs-snapshot"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core"     % "jackson-annotations" % "2.8.10",
  "com.fasterxml.jackson.core"     % "jackson-databind"    % "2.8.10",
  "com.fasterxml.jackson.core"     % "jackson-core"        % "2.8.10",
  "org.apache.commons" % "commons-compress" % "1.4.1",  // upgrading a transitive dependency to avoid security warnings
  "org.apache.httpcomponents" % "httpclient" % "4.5.3",  // upgrading a transitive dependency to avoid security warnings
  "ch.qos.logback" % "logback-classic" % "1.2.2",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.4",
  "com.google.api-client" % "google-api-client" % "1.20.0" excludeAll ExclusionRule(organization = "com.google.guava"),
  "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev53-1.20.0" excludeAll ExclusionRule(organization = "com.google.guava"),
  "com.h2database" % "h2" % "1.3.175",
  "com.typesafe.akka" %% "akka-actor" % akkaV,
  "com.typesafe.akka" %% "akka-stream" % akkaV,
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV,
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.9",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.1",
  "com.typesafe.slick" %% "slick" % "3.2.0",
  "com.zaxxer" % "HikariCP" % "2.3.9",
  "mysql" % "mysql-connector-java" % "5.1.42",
  "org.broadinstitute" %% "cromwell-wdl" % cromwellVersion,
  "org.broadinstitute.dsde.workbench" %% "workbench-util" % "0.3-f3ce961",
  "org.mongodb" %% "casbah" % "3.1.1",
  "org.scalaz" %% "scalaz-core" % "7.2.18",
  "org.webjars" % "swagger-ui"  % "2.2.10-1",
  //---------- Test libraries -------------------//
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "org.mock-server" % "mockserver-netty" % "3.10.2" % "test"
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
  case x if x.endsWith("io.netty.versions.properties") => 
    MergeStrategy.discard
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
