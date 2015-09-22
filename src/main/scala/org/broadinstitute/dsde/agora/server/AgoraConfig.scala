
package org.broadinstitute.dsde.agora.server

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType.EntityType
import org.broadinstitute.dsde.agora.server.webservice.routes.{AgoraDirectives, MockAgoraDirectives, OpenIdConnectDirectives}
import scala.slick.jdbc.JdbcBackend.Database

object AgoraConfig {
  private val config: Config = ConfigFactory.load()

  // Environments
  val LocalEnvironment = "local"
  val DevEnvironment = "dev"
  val StagingEnvironment = "staging"
  val ProdEnvironment = "prod"

  val Environments = List[String](LocalEnvironment, DevEnvironment, StagingEnvironment, ProdEnvironment)

  lazy val environment = config.as[Option[String]]("environment").getOrElse(LocalEnvironment)
  if (!Environments.contains(AgoraConfig.environment))
    throw new IllegalArgumentException("Illegal environment '" + AgoraConfig.environment + "' specified.")

  var authenticationDirectives: AgoraDirectives = _
  var usesEmbeddedMongo: Boolean = _
  lazy val mockAuthenticatedUserEmail = config.as[Option[String]]("mockAuthenticatedUserEmail").getOrElse("noone@broadinstitute.org")

  // Local
  if (environment.equals(LocalEnvironment)) {
    authenticationDirectives = MockAgoraDirectives
    usesEmbeddedMongo = true
  }

  // Dev
  if (environment.equals(DevEnvironment)) {
    authenticationDirectives = OpenIdConnectDirectives
    usesEmbeddedMongo = false
  }

  // CI
  if (environment.equals(StagingEnvironment)) {
    authenticationDirectives = OpenIdConnectDirectives
    usesEmbeddedMongo = false
  }

  // Prod
  if (environment.equals(ProdEnvironment)) {
    authenticationDirectives = OpenIdConnectDirectives
    usesEmbeddedMongo = false
  }


  // Agora
  lazy val serverInstanceName = config.as[String]("instance.name")
  private lazy val scheme = config.as[Option[String]]("webservice.scheme").getOrElse("http")
  private lazy val host = config.as[Option[String]]("webservice.host").getOrElse("localhost")
  lazy val port = config.as[Option[Int]]("webservice.port").getOrElse(8000)
  private lazy val embeddedUrlPort = config.as[Option[Int]]("embeddedUrl.port")
  private lazy val embeddedUrlPortStr = embeddedUrlPort match { case None => "" case x: Some[Int] => ":" + x.get }
  private lazy val baseUrl = scheme + "://" + host + embeddedUrlPortStr + "/"
  lazy val methodsRoute = config.as[Option[String]]("methods.route").getOrElse("methods")
  lazy val methodsUrl = baseUrl + methodsRoute + "/"
  lazy val configurationsRoute = config.as[Option[String]]("configurations.route").getOrElse("configurations")
  lazy val configurationsUrl = baseUrl + configurationsRoute + "/"
  lazy val webserviceInterface = config.as[Option[String]]("webservice.interface").getOrElse("0.0.0.0")
  lazy val supervisorLogging = config.as[Option[Boolean]]("supervisor.logging").getOrElse(true)
  lazy val kamonInstrumentation = config.as[Option[Boolean]]("kamon.instrumentation").getOrElse(true)

  // Mongo
  lazy val mongoDbHost = config.as[Option[String]]("mongodb.host").getOrElse("localhost")
  lazy val mongoDbPort = config.as[Option[Int]]("mongodb.port").getOrElse(27017)
  lazy val mongoDbUser = config.as[Option[String]]("mongodb.user")
  lazy val mongoDbPassword = config.as[Option[String]]("mongodb.password")
  lazy val mongoDbDatabase = config.as[Option[String]]("mongodb.db").getOrElse("agora")

  // SQL
  lazy val sqlDatabase = Database.forConfig("sqlDatabase")

  // Google Credentials
  lazy val gcsProjectId = config.as[String]("gcs.project.id")
  lazy val gcsServiceAccountUserEmail = config.as[String]("gcs.service.account.email")
  lazy val gcServiceAccountP12KeyFile = config.as[String]("gcs.service.account.p12.key.file")

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
  }

  def urlFromType(entityType: Option[EntityType]): String = {
    entityType match {
      case Some(AgoraEntityType.Task) | Some(AgoraEntityType.Workflow) => methodsUrl
      case Some(AgoraEntityType.Configuration) => configurationsUrl
      case _ => baseUrl
    }
  }
}
