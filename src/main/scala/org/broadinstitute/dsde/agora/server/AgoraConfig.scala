
package org.broadinstitute.dsde.agora.server

import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType.EntityType

object AgoraConfig {
  private val config: Config = ConfigFactory.load()

  lazy val serverInstanceName = config.as[String]("instance.name")
  private lazy val scheme = config.as[Option[String]]("webservice.scheme").getOrElse("http")
  private lazy val host = config.as[Option[String]]("webservice.host").getOrElse("localhost")
  lazy val port = config.as[Option[Int]]("webservice.port").getOrElse(8000)
  private lazy val baseUrl = scheme + "://" + host + ":" + port + "/"
  lazy val methodsRoute = config.as[Option[String]]("methods.route").getOrElse("methods")
  lazy val methodsUrl = baseUrl + methodsRoute + "/"
  lazy val configurationsRoute = config.as[Option[String]]("configurations.route").getOrElse("configurations")
  lazy val configurationsUrl = baseUrl + configurationsRoute +"/"
  lazy val webserviceInterface = config.as[Option[String]]("webservice.interface").getOrElse("0.0.0.0")

  lazy val mongoDbHost = config.as[Option[String]]("mongodb.host").getOrElse("localhost")
  lazy val mongoDbPort = config.as[Option[Int]]("mongodb.port").getOrElse(27017)
  lazy val mongoDbUser = config.as[Option[String]]("mongodb.user")
  lazy val mongoDbPassword = config.as[Option[String]]("mongodb.password")

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
