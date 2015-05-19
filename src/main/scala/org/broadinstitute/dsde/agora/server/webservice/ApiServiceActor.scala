package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceHandlerProps
import org.broadinstitute.dsde.agora.server.webservice.validation.AgoraValidationRejection
import spray.routing._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

import scala.reflect.runtime.universe._

object ApiServiceActor {
  def props: Props = Props(classOf[ApiServiceActor])
}

class ApiServiceActor extends HttpServiceActor {
  override def actorRefFactory = context

  trait ActorRefFactoryContext {
    def actorRefFactory = context
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with ServiceHandlerProps with AgoraOpenAMDirectives

  def possibleRoutes = methodsService.routes ~ swaggerService.routes ~
    get {
      pathSingleSlash {
        getFromResource("swagger/index.html")
      } ~ getFromResourceDirectory("swagger/") ~ getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/2.0.24/")
    }

  def receive = runRoute(possibleRoutes)

  implicit val rejectionHandler = RejectionHandler {
    case AgoraValidationRejection(validation) :: _ => complete(BadRequest, validation)
  }

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[MethodsService])

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
