package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{MockAuthorizationProvider, AuthorizationProvider}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsAuthorizationProvider
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.routes.{MockAgoraDirectives, AgoraOpenAMDirectives}
import spray.http.StatusCodes._
import spray.routing._
import spray.util.LoggingContext

import scala.reflect.runtime.universe._

object ApiServiceActor {
  def props(environment: String): Props = Props(classOf[ApiServiceActor], environment)
}

class ApiServiceActor(environment: String) extends HttpServiceActor {

  override def actorRefFactory = context

  trait ActorRefFactoryContext {
    def actorRefFactory = context
  }

  val methodsService = AgoraConfig.useOpenAMAuthentication(environment) match {
    case true => new MethodsService(authorizationProvider()) with ActorRefFactoryContext with AgoraOpenAMDirectives
    case _    => new MethodsService(authorizationProvider()) with ActorRefFactoryContext with MockAgoraDirectives
  }

  val configurationsService = AgoraConfig.useOpenAMAuthentication(environment) match {
    case true => new ConfigurationsService(authorizationProvider()) with ActorRefFactoryContext with AgoraOpenAMDirectives
    case _    => new ConfigurationsService(authorizationProvider()) with ActorRefFactoryContext with MockAgoraDirectives
  }

  def authorizationProvider() : AuthorizationProvider = {
    AgoraConfig.useGcsAuthorizationProvider(environment) match {
      case true => GcsAuthorizationProvider
      case _    => MockAuthorizationProvider
    }
  }

  def possibleRoutes = methodsService.routes ~ configurationsService.routes ~ swaggerService.routes ~
    get {
      pathSingleSlash {
        getFromResource("swagger/index.html")
      } ~ getFromResourceDirectory("swagger/") ~ getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/2.0.24/")
    }

  def receive = runRoute(possibleRoutes)

  implicit def routeExceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: IllegalArgumentException => complete(BadRequest, e.getMessage)
      case _ => complete(InternalServerError, "Something went wrong, but the error is unspecified.")
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
