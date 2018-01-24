package org.broadinstitute.dsde.agora.server.webservice

import java.security.Permissions

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{OneForOneStrategy, Props}
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AdminSweeper, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper.Sweep
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraException, ValidationException}
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghService
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.parboiled.common.FileUtils
import spray.http.{ContentType, HttpEntity, MediaTypes}
import spray.http.StatusCodes._
import spray.routing._
import spray.util.LoggingContext

import scala.concurrent.duration._
import scala.reflect.runtime.universe._

object ApiServiceActor {
  def props(permissionsDataSource: PermissionsDataSource): Props = Props(classOf[ApiServiceActor], permissionsDataSource)
}

class ApiServiceActor(permissionsDataSource: PermissionsDataSource) extends HttpServiceActor with LazyLogging {

  override def actorRefFactory = context

  trait ActorRefFactoryContext {
    def actorRefFactory = context
  }

  // JSON Serialization Support
  import spray.httpx.SprayJsonSupport._
  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._

  override val supervisorStrategy =
    OneForOneStrategy(loggingEnabled = AgoraConfig.supervisorLogging) {
      case e: Throwable =>
        logger.error("ApiServiceActor child threw exception. Child will be restarted", e)
        Restart
    }

  /**
   * Firecloud system maintains it's set of admins as a google group.
   *
   * If such a group is specified in config, poll it at regular intervals
   *   to synchronize the admins defined in our users table
   */
  AgoraConfig.adminGoogleGroup match {
    case Some(group) =>
      import context.dispatcher
      val adminSweeper = actorRefFactory.actorOf(AdminSweeper.props(AdminSweeper.adminsGoogleGroupPoller, permissionsDataSource))
      val adminScheduler =
        context.system.scheduler.schedule(5 seconds, AgoraConfig.adminSweepInterval minutes, adminSweeper, Sweep)
    case None =>
  }

  val methodsService = new MethodsService(permissionsDataSource) with ActorRefFactoryContext
  val configurationsService = new ConfigurationsService(permissionsDataSource) with ActorRefFactoryContext

  val ga4ghService = new Ga4ghService(permissionsDataSource) with ActorRefFactoryContext

  def withResourceFileContents(path: String)(innerRoute: String => Route): Route =
    innerRoute( FileUtils.readAllTextFromResource(path) )

  def possibleRoutes =  options{ complete(OK) } ~ ga4ghService.routes ~ methodsService.routes ~ configurationsService.routes ~
    swaggerService

  def receive = runRoute(possibleRoutes)

  implicit def routeExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: IllegalArgumentException => complete(BadRequest, e)
      case ve: ValidationException => complete(BadRequest, ve)
      case ex: Throwable => {
        logger.error(ex.getMessage)
        complete(InternalServerError, AgoraException("Something went wrong, but the error is unspecified."))
      }
    }

  implicit val routeRejectionHandler =
    RejectionHandler {
      case MalformedRequestContentRejection(message, cause) :: _ => complete(BadRequest, AgoraException(message=message, cause, BadRequest))
      case ValidationRejection(message, cause) :: _ => complete(BadRequest, AgoraException(message=message, cause, BadRequest))
  }

  private val swaggerUiPath = "META-INF/resources/webjars/swagger-ui/2.2.10-1"

  val swaggerService = {
    path("") {
      get {
        parameter("url") {urlparam =>
          requestUri {uri =>
            redirect(uri.withQuery(Map.empty[String,String]), MovedPermanently)
          }
        } ~
          serveIndex()
      }
    } ~
    path("agora.yaml") {
      get {
        withResourceFileContents("swagger/agora.yaml") { apiDocs =>
          complete(apiDocs)
        }
      }
    } ~
    // We have to be explicit about the paths here since we're matching at the root URL and we don't
    // want to catch all paths lest we circumvent Spray's not-found and method-not-allowed error
    // messages.
    (pathSuffixTest("o2c.html") | pathSuffixTest("swagger-ui.js")
      | pathPrefixTest("css" /) | pathPrefixTest("fonts" /) | pathPrefixTest("images" /)
      | pathPrefixTest("lang" /) | pathPrefixTest("lib" /)) {
        get {
          getFromResourceDirectory(swaggerUiPath)
        }
      }
  }

  private def serveIndex(): Route = {
    withResourceFileContents(swaggerUiPath + "/index.html") { indexHtml =>
      complete {
        val swaggerOptions =
          """
            |        validatorUrl: null,
            |        apisSorter: "alpha",
            |        operationsSorter: "alpha",
          """.stripMargin

        HttpEntity(ContentType(MediaTypes.`text/html`),
          indexHtml
            .replace("your-client-id", AgoraConfig.SwaggerConfig.clientId)
            .replace("your-realms", AgoraConfig.SwaggerConfig.realm)
            .replace("your-app-name", AgoraConfig.SwaggerConfig.appName)
            .replace("scopeSeparator: \",\"", "scopeSeparator: \" \"")
            .replace("jsonEditor: false,", "jsonEditor: false," + swaggerOptions)
            .replace("url = \"http://petstore.swagger.io/v2/swagger.json\";",
              "url = '/agora.yaml';")
        )
      }
    }
  }

}
