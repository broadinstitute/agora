package org.broadinstitute.dsde.agora.server

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType.EntityType
import org.broadinstitute.dsde.agora.server.webservice.routes.{AgoraDirectives, MockAgoraDirectives, OpenIdConnectDirectives}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object AgoraConfig {

  private val config: Config = ConfigFactory.load()

  // Environments
  val TestEnvironment = "test"

  var authenticationDirectives: AgoraDirectives = _
  var usesEmbeddedMongo: Boolean = _
  lazy val mockAuthenticatedUserEmail = config.as[Option[String]]("mockAuthenticatedUserEmail").getOrElse("noone@broadinstitute.org")

  val environment = config.as[Option[String]]("environment")
  environment match {
    case Some(env) if env == TestEnvironment =>
      authenticationDirectives = MockAgoraDirectives
      usesEmbeddedMongo = true
    case _ =>
      authenticationDirectives = OpenIdConnectDirectives
      usesEmbeddedMongo = false
  }

  // Agora
  private lazy val embeddedUrlPort = config.as[Option[Int]]("embeddedUrl.port")
  private lazy val embeddedUrlPortStr = embeddedUrlPort match {
    case None => ""
    case x: Some[Int] => ":" + x.get
  }
  private lazy val baseUrl = scheme + "://" + host + embeddedUrlPortStr + "/" + "api" + "/" + version + "/"
  private lazy val scheme = config.as[Option[String]]("webservice.scheme").getOrElse("http")
  lazy val host = config.as[Option[String]]("webservice.host").getOrElse("localhost")
  lazy val port = config.as[Option[Int]]("webservice.port").getOrElse(8000)
  lazy val version = config.as[Option[String]]("webservice.version").getOrElse("v1")
  lazy val methodsRoute = config.as[Option[String]]("methods.route").getOrElse("methods")
  lazy val methodsUrl = baseUrl + methodsRoute + "/"
  lazy val configurationsRoute = config.as[Option[String]]("configurations.route").getOrElse("configurations")
  lazy val configurationsUrl = baseUrl + configurationsRoute + "/"
  lazy val webserviceInterface = config.as[Option[String]]("webservice.interface").getOrElse("0.0.0.0")
  lazy val supervisorLogging = config.as[Option[Boolean]]("supervisor.logging").getOrElse(true)
  lazy val adminSweepInterval = config.as[Option[Int]]("admin.sweep.interval").getOrElse(15)

  // Mongo
  lazy val mongoDbHosts = config.as[List[String]]("mongodb.hosts")
  lazy val mongoDbPorts = config.as[List[Int]]("mongodb.ports")
  lazy val mongoDbUser = config.as[Option[String]]("mongodb.user")
  lazy val mongoDbPassword = config.as[Option[String]]("mongodb.password")
  lazy val mongoDbDatabase = config.as[Option[String]]("mongodb.db").getOrElse("agora")

  // SQL
  lazy val sqlDatabase = DatabaseConfig.forConfig[JdbcProfile]("sqlDatabase")

  // Google Credentials
  lazy val gcsProjectId = config.as[String]("gcs.project.id")
  lazy val gcsServiceAccountEmail = config.as[String]("gcs.service.account.email")
  lazy val gcsServiceAccountPemFile = config.as[String]("gcs.service.account.pem.file")
  lazy val gcsUserEmail = config.as[String]("gcs.user.email")
  lazy val adminGoogleGroup = config.as[Option[String]]("admin.google.group")

  //Config Settings
  object SwaggerConfig {
    private val swagger = config.getConfig("swagger")
    lazy val apiVersion = swagger.getString("apiVersion")
    lazy val swaggerVersion = swagger.getString("swaggerVersion")
    lazy val info = swagger.getString("info")
    lazy val description = swagger.getString("description")
    lazy val termsOfServiceUrl = swagger.getString("termsOfServiceUrl")
    lazy val contact = swagger.getString("contact")
    lazy val license = swagger.getString("license")
    lazy val licenseUrl = swagger.getString("licenseUrl")
    lazy val baseUrl = swagger.getString("baseUrl")
    lazy val apiDocs = swagger.getString("apiDocs")
    lazy val clientId = swagger.as[Option[String]]("clientId").getOrElse("clientId")
    lazy val realm = gcsProjectId
    lazy val appName = gcsProjectId
  }

  def urlFromType(entityType: Option[EntityType]): String = {
    entityType match {
      case Some(AgoraEntityType.Task) | Some(AgoraEntityType.Workflow) => methodsUrl
      case Some(AgoraEntityType.Configuration) => configurationsUrl
      case _ => baseUrl
    }
  }
}
