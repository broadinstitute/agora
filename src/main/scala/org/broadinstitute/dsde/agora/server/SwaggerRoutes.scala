package org.broadinstitute.dsde.agora.server

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.Flow
import akka.util.ByteString

trait SwaggerRoutes {
  private val swaggerUiPath = "META-INF/resources/webjars/swagger-ui/4.5.2"

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
      (pathPrefixTest("swagger-ui") | pathPrefixTest("oauth2") | pathSuffixTest("js")
        | pathSuffixTest("css") | pathPrefixTest("favicon")) {
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
        |        operationsSorter: "alpha"
      """.stripMargin

    mapResponseEntity { entityFromJar =>
      entityFromJar.transformDataBytes(Flow.fromFunction[ByteString, ByteString] { original: ByteString =>
        ByteString(original.utf8String
          .replace("""url: "https://petstore.swagger.io/v2/swagger.json"""", "url: '/agora.yaml'")
          .replace("""layout: "StandaloneLayout"""", s"""layout: "StandaloneLayout", $swaggerOptions""")
          .replace("window.ui = ui", s"""ui.initOAuth({
                                        |        clientId: "${AgoraConfig.SwaggerConfig.clientId}",
                                        |        clientSecret: "${AgoraConfig.SwaggerConfig.realm}",
                                        |        realm: "${AgoraConfig.SwaggerConfig.realm}",
                                        |        appName: "${AgoraConfig.SwaggerConfig.appName}",
                                        |        scopeSeparator: " ",
                                        |        additionalQueryStringParams: {}
                                        |      })
                                        |      window.ui = ui
                                        |      """.stripMargin)
        )})
    } {
      getFromResource(swaggerUiPath + "/index.html")
    }
  }
}
