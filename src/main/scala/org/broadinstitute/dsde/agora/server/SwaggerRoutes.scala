package org.broadinstitute.dsde.agora.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import akka.util.ByteString

trait SwaggerRoutes {
  private val swaggerUiPath = "META-INF/resources/webjars/swagger-ui/2.2.10-1"

  val swaggerRoutes: server.Route = {
    path("") {
      get {
        parameter("url") {urlparam =>
          extractUri {uri =>
            redirect(uri.withRawQueryString(""), StatusCodes.MovedPermanently)
          }
        } ~
          serveIndex()
      }
    } ~
      path("agora.yaml") {
        get {
          getFromResource("swagger/agora.yaml")
        }
      } ~
      // We have to be explicit about the paths here since we're matching at the root URL and we don't
      // want to catch all paths lest we circumvent Spray's not-found and method-not-allowed error
      // messages.
      (pathSuffixTest("o2c.html") | pathSuffixTest("swagger-ui.js")
        | pathPrefixTest("css") | pathPrefixTest("fonts") | pathPrefixTest("images")
        | pathPrefixTest("lang") | pathPrefixTest("lib")) {
        get {
          getFromResourceDirectory(swaggerUiPath)
        }
      }
  }

  private def serveIndex(): server.Route = {
    val swaggerOptions =
      """
        |        validatorUrl: null,
        |        apisSorter: "alpha",
        |        operationsSorter: "alpha",
      """.stripMargin

    mapResponseEntity { entityFromJar =>
      entityFromJar.transformDataBytes(Flow.fromFunction[ByteString, ByteString] { original: ByteString =>
        ByteString(original.utf8String
          .replace("your-client-id", AgoraConfig.SwaggerConfig.clientId)
          .replace("your-realms", AgoraConfig.SwaggerConfig.realm)
          .replace("your-app-name", AgoraConfig.SwaggerConfig.appName)
          .replace("scopeSeparator: \",\"", "scopeSeparator: \" \"")
          .replace("jsonEditor: false,", "jsonEditor: false," + swaggerOptions)
          .replace("url = \"http://petstore.swagger.io/v2/swagger.json\";", "url = '/agora.yaml';")
        )})
    } {
      getFromResource(swaggerUiPath + "/index.html")
    }
  }
}
