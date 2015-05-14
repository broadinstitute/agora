
package org.broadinstitute.dsde.agora.server

import java.io.File
import net.ceedubs.ficus.Ficus._
import com.typesafe.config.{Config, ConfigFactory}

object AgoraConfig {
  private val config: Config = ConfigFactory.load()
  private val agoraConfig: Config = ConfigFactory.parseFile(new File("/etc/agora.conf"))

  private val appConfig = agoraConfig.withFallback(config)
  lazy val serverInstanceName = appConfig.as[String]("instance.name")
  private lazy val scheme = appConfig.as[Option[String]]("webservice.scheme").getOrElse("http")
  private lazy val host = appConfig.as[Option[String]]("webservice.host").getOrElse("localhost")
  lazy val port = appConfig.as[Option[Int]]("webservice.port").getOrElse(8000)
  private lazy val baseUrl = scheme + "://" + host + ":" + port + "/"
  lazy val methodsRoute = appConfig.as[Option[String]]("methods.route").getOrElse("methods")
  lazy val methodsUrl = baseUrl + methodsRoute + "/"
  lazy val webserviceInterface = appConfig.as[Option[String]]("webservice.interface").getOrElse("0.0.0.0")

  lazy val mongoDbHost = appConfig.as[Option[String]]("mongodb.host").getOrElse("localhost")
  lazy val mongoDbPort = appConfig.as[Option[Int]]("mongodb.port").getOrElse(27017)
  lazy val mongoDbUser = appConfig.as[Option[String]]("mongodb.user")
  lazy val mongoDbPassword = appConfig.as[Option[String]]("mongodb.password")

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

}
