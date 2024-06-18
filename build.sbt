import sbtassembly.MergeStrategy

name := "Agora"

organization := "org.broadinstitute"

scalaVersion := "2.13.9"

// Ported from Cromwell 2019-11-19
// The disabled ones are not too crazy to get compliant with, but more work than I want to do just now (AEN)
scalacOptions := Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Ywarn-unused:imports",
//  "-Xlint:adapted-args",
  "-Xlint:constant",
  "-Xlint:delayedinit-select",
  "-Xlint:doc-detached",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
//  "-Xlint:missing-interpolator",
  "-Xlint:nullary-unit",
  "-Xlint:option-implicit",
  "-Xlint:package-object-classes",
  "-Xlint:poly-implicit-overload",
  "-Xlint:private-shadow",
  "-Xlint:stars-align",
  "-Xlint:type-parameter-shadow",
//  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
//  "-Ywarn-value-discard",
  "-Xlint:inaccessible",
  "-Ywarn-unused:implicits",
  "-Ywarn-unused:privates",
  "-Ywarn-unused:locals",
  "-Ywarn-unused:patvars"
)

val akkaV = "2.6.9"
val akkaHttpV = "10.2.9"
val jacksonV = "2.11.3"
val slickV = "3.3.3"
val testcontainersScalaV = "0.38.4"

val artifactory = "https://broadinstitute.jfrog.io/broadinstitute/"

resolvers += "artifactory-releases" at artifactory + "libs-release"

resolvers += "artifactory-snapshots" at artifactory + "libs-snapshot"

val workbenchLibV = "a6ad7dc"
val workbenchOauth2V = s"0.7-$workbenchLibV"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "com.google.api-client" % "google-api-client" % "1.25.0" excludeAll ExclusionRule(organization = "com.google.guava"),
  "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev118-1.25.0" excludeAll
    ExclusionRule(organization = "com.google.guava"),
  "com.typesafe.akka" %% "akka-actor-typed" % akkaV,
  "com.typesafe.akka" %% "akka-http" % akkaHttpV,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
  "com.typesafe.akka" %% "akka-slf4j" % akkaV,
  // It was necessary to explicitly specify the version for akka-stream to appease Akka as it complains otherwise with:
  // "Akka version MUST be the same across all modules of Akka that you are using."
  // This will not add a direct dependency on akka-stream, but will force the revision to be akkaV.
  "com.typesafe.akka" %% "akka-stream-typed" % akkaV,
  "com.typesafe" % "config" % "1.4.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.typesafe.slick" %% "slick" % slickV,
  "com.typesafe.slick" %% "slick-hikaricp" % slickV,
  "mysql" % "mysql-connector-java" % "8.0.31" exclude("com.google.protobuf", "protobuf-java"),

  // ficus was being pulled in transitively from wdl-draft2 previously, now made explicit
  "com.iheart" %% "ficus" % "1.5.0",
  "org.broadinstitute.dsde.workbench" %% "workbench-util" % "0.6-65bba14",
  "org.mongodb.scala" %% "mongo-scala-driver" % "4.1.0",

  // Not used directly, but keep Jackson up to date plus main+test synced for IntelliJ
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonV,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonV % Test,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonV % Test,

  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaV % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV % Test,
  "org.scalatest" %% "scalatest" % "3.2.2" % Test,
  "org.mock-server" % "mockserver-netty" % "5.11.1" % Test,
  "com.dimafeng" %% "testcontainers-scala-mysql" % testcontainersScalaV % Test,
  "com.dimafeng" %% "testcontainers-scala-mongodb" % testcontainersScalaV % Test,
  "org.flywaydb" % "flyway-core" % "7.3.2" % Test,
  "org.broadinstitute.dsde.workbench" % "sam-client_2.12" % "0.1-61135c7",      // Should become available for 2.13 once a release happens ( https://github.com/broadinstitute/sam/pull/491 )
  "org.broadinstitute.cromwell" % "cromwell-client_2.12" % "0.1-8b413b45f-SNAP", // Contains only Java, pinning on 2.12
  "org.broadinstitute.dsde.workbench" %% "workbench-oauth2" % workbenchOauth2V excludeAll(
    // these included libs cause conflicts during sbt assembly
    ExclusionRule(organization = "com.google.protobuf"),
    ExclusionRule(organization = "com.squareup.okhttp3")),
)

// Flyway may be run with system properties:
// i.e: sbt -Dflyway.url=jdbc:mysql://DB_HOST:DB_PORT/DB_NAME -Dflyway.user=root -Dflyway.password=abc123

flywayCleanDisabled := true

// We need to fork the tests and provide the correct config so that users do not accidentally
// provide a config that points to a real database.
javaOptions in Test := Seq("-Dconfig.file=src/test/resources/application.conf")
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
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x if x.endsWith("/OSGI-INF/MANIFEST.MF") => MergeStrategy.first
  case x if x.contains("bouncycastle") => MergeStrategy.first
  case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", xs@_*) =>
    xs map {
      _.toLowerCase
    } match {
      case "manifest.mf" :: Nil | "index.list" :: Nil | "dependencies" :: Nil =>
        MergeStrategy.discard
      case ps@_ :: _ if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: _ =>
        MergeStrategy.discard
      case "spring.tooling" :: _ =>
        MergeStrategy.discard
      case "com.google.guava" :: _ =>
        MergeStrategy.discard
      case "services" :: _ =>
        MergeStrategy.filterDistinctLines
      case "spring.schemas" :: Nil | "spring.handlers" :: Nil =>
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

MinnieKenny.testSettings
