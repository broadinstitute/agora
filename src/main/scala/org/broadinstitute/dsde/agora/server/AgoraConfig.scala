package org.broadinstitute.dsde.agora.server

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType.EntityType
import org.broadinstitute.dsde.agora.server.webservice.routes.{AgoraDirectives, MockAgoraDirectives, OpenIdConnectDirectives}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

import scala.util.{Failure, Success, Try}

object AgoraConfig {

  private val config: Config = ConfigFactory.load()

  // Environments
  val TestEnvironment = "test"

  lazy val mockAuthenticatedUserEmail: String =
    config.getOrElse("mockAuthenticatedUserEmail", "noone@broadinstitute.org")
  lazy val mockMixedCaseAuthenticatedUserEmail: String = "UPPER" + mockAuthenticatedUserEmail
  lazy val mockAccessToken: String = config.getOrElse("mockAccessToken", "ya.not.a.real.token")

  lazy val environment: Option[String] = config.getAs[String]("environment")
  lazy val (authenticationDirectives: AgoraDirectives, mongoDbTestContainerEnabled: Boolean) =
    environment match {
      case Some(env) if env == TestEnvironment =>
        (MockAgoraDirectives, true)
      case _ =>
        (OpenIdConnectDirectives, false)
    }

  // Agora
  private lazy val embeddedUrlPort = config.getAs[Int]("embeddedUrl.port")
  private lazy val embeddedUrlPortStr = embeddedUrlPort match {
    case None => ""
    case x: Some[Int] => ":" + x.get
  }
  private lazy val baseUrl = scheme + "://" + host + embeddedUrlPortStr + "/" + "api" + "/" + version + "/"
  private lazy val scheme = config.getOrElse("webservice.scheme", "http")
  lazy val host: String = config.getOrElse("webservice.host", "localhost")
  lazy val port: Int = config.getOrElse("webservice.port", 8000)
  lazy val version: String = config.getOrElse("webservice.version", "v1")
  lazy val methodsRoute: String = config.getOrElse("methods.route", "methods")
  lazy val methodsUrl: String = baseUrl + methodsRoute + "/"
  lazy val configurationsRoute: String = config.getOrElse("configurations.route", "configurations")
  lazy val configurationsUrl: String = baseUrl + configurationsRoute + "/"
  lazy val webserviceInterface: String = config.getOrElse("webservice.interface", "0.0.0.0")
  lazy val supervisorLogging: Boolean = config.getOrElse("supervisor.logging", true)
  lazy val adminSweepInterval: Int = config.getOrElse("admin.sweep.interval", 15)

  // Mongo
  lazy val mongoDbHosts: List[String] = config.as[List[String]]("mongodb.hosts")
  lazy val mongoDbPorts: List[Int] = config.as[List[Int]]("mongodb.ports")
  lazy val mongoDbUser: Option[String] = config.getAs[String]("mongodb.user")
  lazy val mongoDbPassword: Option[String] = config.getAs[String]("mongodb.password")
  lazy val mongoDbDatabase: String = config.getOrElse("mongodb.db", "agora")
  // not part of the mongo connection, so kept in a separate config tree
  lazy val mongoAliasBatchSize: Int = config.getOrElse("mongo.aliasBatchSize", 5000)

  // Womtool-as-a-Service (Waas)
  lazy val waasServer: Option[String] = config.getAs[String]("waas.server")

  // SQL
  lazy val sqlConfig: Config = config.getConfig("sqlDatabase")
  lazy val sqlDatabase = DatabaseConfig.forConfig[JdbcProfile]("", sqlConfig)
  // not part of the SQL connection, so kept in a separate config tree
  lazy val sqlAliasBatchSize: Int = config.getOrElse("sql.aliasBatchSize", 100)


  // Google Credentials
  lazy val googleProjectId: String = config.as[String]("google.project.id")
  lazy val googleServiceAccountEmail: String = config.as[String]("google.service.account.email")
  lazy val googleServiceAccountPemFile: String = config.as[String]("google.service.account.pem.file")
  lazy val googleUserEmail: String = config.as[String]("google.user.email")
  lazy val adminGoogleGroup: Option[String] = config.getAs[String]("admin.google.group")

  // sam
  lazy val samUrl: String = config.as[String]("sam.url")

  //Config Settings
  object SwaggerConfig {
    private val swagger = config.getConfig("swagger")
    lazy val apiVersion: String = swagger.getString("apiVersion")
    lazy val swaggerVersion: String = swagger.getString("swaggerVersion")
    lazy val info: String = swagger.getString("info")
    lazy val description: String = swagger.getString("description")
    lazy val termsOfServiceUrl: String = swagger.getString("termsOfServiceUrl")
    lazy val contact: String = swagger.getString("contact")
    lazy val license: String = swagger.getString("license")
    lazy val licenseUrl: String = swagger.getString("licenseUrl")
    lazy val baseUrl: String = swagger.getString("baseUrl")
    lazy val apiDocs: String = swagger.getString("apiDocs")
    lazy val clientId: String = swagger.getOrElse("clientId", "clientId")
    lazy val realm: String = googleProjectId
    lazy val appName: String = googleProjectId
  }

  // GA4GH
  object GA4GH {
    private lazy val baseGA4GHUrl = "%s://%s%s/ga4gh/%s/".format(scheme, host, embeddedUrlPortStr, version)
    def toolUrl(id: String, versionId: String, toolType: String): String = baseGA4GHUrl + "tools/%s/versions/%s/%s/descriptor".format(id, versionId, toolType)
  }

  def urlFromType(entityType: Option[EntityType]): String = {
    entityType match {
      case Some(AgoraEntityType.Task) | Some(AgoraEntityType.Workflow) => methodsUrl
      case Some(AgoraEntityType.Configuration) => configurationsUrl
      case _ => baseUrl
    }
  }

  implicit class EnhancedScalaConfig(val config: Config) extends AnyVal {
    def getConfigOption(key: String): Option[Config] = getOption(key, config.getConfig)
    def getStringOption(key: String): Option[String] = getOption(key, config.getString)
    def getBooleanOption(key: String): Option[Boolean] = getOption(key, config.getBoolean)
    def getIntOption(key: String): Option[Int] = getOption(key, config.getInt)
    def getLongOption(key: String): Option[Long] = getOption(key, config.getLong)
    def getDoubleOption(key: String): Option[Double] = getOption(key, config.getDouble)
    def getStringOr(key: String, default: => String = ""): String = getStringOption(key) getOrElse default
    def getBooleanOr(key: String, default: => Boolean = false): Boolean = getBooleanOption(key) getOrElse default
    def getIntOr(key: String, default: => Int = 0): Int = getIntOption(key) getOrElse default
    def getLongOr(key: String, default: => Long = 0L): Long = getLongOption(key) getOrElse default
    def getDoubleOr(key: String, default: => Double = 0.0): Double = getDoubleOption(key) getOrElse default

    private def getOption[T](key: String, f: String => T): Option[T] = {
      Try(f(key)) match {
        case Success(value) => Option(value)
        case Failure(_: ConfigException.Missing) => None
        case Failure(e) => throw e
      }
    }
  }

}
