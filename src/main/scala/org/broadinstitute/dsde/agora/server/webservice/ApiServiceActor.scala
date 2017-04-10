package org.broadinstitute.dsde.agora.server.webservice

import java.security.Permissions

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{OneForOneStrategy, Props}
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.typesafe.scalalogging.slf4j.LazyLogging
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AdminSweeper, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper.Sweep
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

  def withResourceFileContents(path: String)(innerRoute: String => Route): Route =
    innerRoute( FileUtils.readAllTextFromResource(path) )

  def possibleRoutes =  options{ complete(OK) } ~ methodsService.routes ~ configurationsService.routes ~
    get {
      pathSingleSlash {
        withResourceFileContents("swagger/index.html") { indexHtml =>
          complete {
            HttpEntity(ContentType(MediaTypes.`text/html`),
              indexHtml
                .replace("your-client-id", AgoraConfig.SwaggerConfig.clientId)
                .replace("your-realms", AgoraConfig.SwaggerConfig.realm)
                .replace("your-app-name", AgoraConfig.SwaggerConfig.appName)
            )
          }
        }
      } ~ getFromResourceDirectory("swagger/") ~ getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/2.2.5/")
    }

  def receive = runRoute(possibleRoutes)

  implicit def routeExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: IllegalArgumentException => complete(BadRequest, e.getMessage)
      case ex: Throwable => {
        logger.error(ex.getMessage)
        complete(InternalServerError, s"Something went wrong, but the error is unspecified.")
      }
    }

  implicit val routeRejectionHandler =
    RejectionHandler {
      case MalformedRequestContentRejection(message, cause) :: _ => complete(BadRequest, message)
  }

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[MethodsService], typeOf[ConfigurationsService])
    override def apiVersion = AgoraConfig.SwaggerConfig.apiVersion
    override def baseUrl = AgoraConfig.SwaggerConfig.baseUrl
    override def docsPath = AgoraConfig.SwaggerConfig.apiDocs
    override def actorRefFactory = context
    override def swaggerVersion = AgoraConfig.SwaggerConfig.swaggerVersion

    override def apiInfo = Some(
      new ApiInfo(
        AgoraConfig.SwaggerConfig.info,
        AgoraConfig.SwaggerConfig.description,
        AgoraConfig.SwaggerConfig.termsOfServiceUrl,
        AgoraConfig.SwaggerConfig.contact,
        AgoraConfig.SwaggerConfig.license,
        AgoraConfig.SwaggerConfig.licenseUrl)
    )
  }
}
