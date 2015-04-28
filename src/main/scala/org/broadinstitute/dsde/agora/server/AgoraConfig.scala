
package org.broadinstitute.dsde.agora.server

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}

object AgoraConfig {
  private val config: Config = ConfigFactory.load() 
  private val agoraConfig: Config = ConfigFactory.parseFile(new File("/etc/agora.conf"))

  def appConfig = agoraConfig.withFallback(config)

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
