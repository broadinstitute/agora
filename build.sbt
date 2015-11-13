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
  "cglib" % "cglib-nodep" % "2.2",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.gettyimages" %% "spray-swagger" % "0.5.0",
  "com.github.simplyscala" %% "scalatest-embedmongo" % "0.2.2",
  "com.google.api-client" % "google-api-client" % "1.20.0" excludeAll ExclusionRule(organization = "com.google.guava"),
  "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev53-1.20.0",
  "com.h2database" % "h2" % "1.3.175",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.11",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "com.typesafe.slick" %% "slick" % "3.0.0",
  "com.zaxxer" % "HikariCP" % "2.3.9",
  "io.kamon" %% "kamon-core" % "0.4.0",
  "io.kamon" %% "kamon-akka" % "0.4.0",
  "io.kamon" %% "kamon-spray" % "0.4.0",
  "io.kamon" %% "kamon-system-metrics" % "0.4.0",
  "io.kamon" %% "kamon-statsd" % "0.4.0",
  "io.spray" %% "spray-can" % sprayV,
  "io.spray" %% "spray-client" % sprayV,
  "io.spray" %% "spray-json" % "1.3.1", // NB: Not at sprayV. 1.3.2 does not exist.
  "io.spray" %% "spray-routing" % sprayV,
  "mysql" % "mysql-connector-java" % "5.1.35",
  "net.ceedubs" %% "ficus" % "1.1.2",
  "org.aspectj" % "aspectjweaver" % "1.8.6",
  "org.broadinstitute" %% "cromwell" % "0.9" excludeAll ExclusionRule(organization = "com.gettyimages"),
  "org.broadinstitute.dsde.vault" %% "vault-common" % "0.1-15-bf74315",
  "org.mongodb" %% "casbah" % "2.8.2",
  "org.flywaydb" % "flyway-core" % "3.2.1",
  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "org.webjars" % "swagger-ui" % "2.1.2",
  //---------- Test libraries -------------------//
  "io.spray" %% "spray-testkit" % sprayV % Test,
  "org.scalatest" %% "scalatest" % "2.2.4" % Test
)

Seq(flywaySettings: _*)

//These can be overrided with system properties:
// i.e: sbt -Dflyway.url=jdbc:mysql://DB_HOST:DB_PORT/DB_NAME -Dflyway.user=root -Dflyway.password=abc123
flywayUrl := "jdbc:h2:file:local"

flywayUser := "root"

lazy val safeClean = taskKey[Unit]("DO NOT RUN IN PRODUCTION! Deletes the entire database with flywayClean. DO NOT RUN IN PRODUCTION!")

safeClean := println("""
                       |__        ___    ____  _   _ ___ _   _  ____ _
                       |\ \      / / \  |  _ \| \ | |_ _| \ | |/ ___| |
                       | \ \ /\ / / _ \ | |_) |  \| || ||  \| | |  _| |
                       |  \ V  V / ___ \|  _ <| |\  || || |\  | |_| |_|
                       |   \_/\_/_/   \_\_| \_\_| \_|___|_| \_|\____(_)
                       |
                       |
                       | This will permanently DELETE EVERYTHING in the database.
                       | Do you really want to delete the entire database?
                       | If so, then remove the line:
                       |
                       | `addCommandAlias("flywayClean", "safeClean")`
                       |
                       | in the build.sbt file and rerun `sbt flywayClean`
                       |
                       | """.stripMargin)

// Wrapper around flywayClean for safety.

addCommandAlias("flywayClean", "safeClean")

releaseSettings

shellPrompt := { state => "%s| %s> ".format(GitCommand.prompt.apply(state), version.value) }

javaOptions in Test ++= Seq("-Dconfig.file=" + Option(System.getenv("TEST_CONFIG")).getOrElse("src/test/resources/reference.conf"))

parallelExecution in Test := false

//assembly settings
test in assembly := {}

assemblyJarName in assembly := "agora-" + version.value + ".jar"

logLevel in assembly := Level.Info

packageOptions in assembly += Package.ManifestAttributes("Premain-Class" -> "org.aspectj.weaver.loadtime.Agent")

// Create a new MergeStrategy for aop.xml files
val aopMerge: MergeStrategy = new MergeStrategy {
  val name = "aopMerge"
  import scala.xml._
  import scala.xml.dtd._
  def apply(tempDir: File, path: String, files: Seq[File]): Either[String, Seq[(File, String)]] = {
    val dt = DocType("aspectj", PublicID("-//AspectJ//DTD//EN", "http://www.eclipse.org/aspectj/dtd/aspectj.dtd"), Nil)
    val file = MergeStrategy.createMergeTarget(tempDir, path)
    val xmls: Seq[Elem] = files.map(XML.loadFile)
    val aspectsChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "aspects" \ "_")
    val weaverChildren: Seq[Node] = xmls.flatMap(_ \\ "aspectj" \ "weaver" \ "_")
    val options: String = xmls.map(x => (x \\ "aspectj" \ "weaver" \ "@options").text).mkString(" ").trim
    val weaverAttr = if (options.isEmpty) Null else new UnprefixedAttribute("options", options, Null)
    val aspects = new Elem(null, "aspects", Null, TopScope, false, aspectsChildren: _*)
    val weaver = new Elem(null, "weaver", weaverAttr, TopScope, false, weaverChildren: _*)
    val aspectj = new Elem(null, "aspectj", Null, TopScope, false, aspects, weaver)
    XML.save(file.toString, aspectj, "UTF-8", xmlDecl = false, dt)
    IO.append(file, IO.Newline.getBytes(IO.defaultCharset))
    Right(Seq(file -> path))
  }
}

assemblyMergeStrategy in assembly := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps@_*) if Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last) =>
    MergeStrategy.rename
  case PathList("META-INF", "aop.xml") =>
    aopMerge
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