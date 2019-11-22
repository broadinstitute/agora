package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorRef
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging.LogLevel
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.SwaggerRoutes
import org.broadinstitute.dsde.agora.server.business.AgoraBusinessExecutionContext
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.exceptions._
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghService
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.permissions.{EntityPermissionsService, MultiEntityPermissionsService, NamespacePermissionsService}
import org.broadinstitute.dsde.workbench.model.{ErrorReport, WorkbenchException, WorkbenchExceptionWithErrorReport}
import org.broadinstitute.dsde.agora.server.errorReportSource
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}

object ApiService extends LazyLogging with SprayJsonSupport with DefaultJsonProtocol {

  // Required for marshalling errors:
  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import org.broadinstitute.dsde.workbench.model.ErrorReportJsonSupport._

  def handleExceptionsAndRejections: Directive[Unit] = handleExceptions(exceptionHandler) & handleRejections(rejectionHandler)

  val exceptionHandler: ExceptionHandler = {
    ExceptionHandler {
      case withErrorReport: WorkbenchExceptionWithErrorReport =>
        complete(withErrorReport.errorReport.statusCode.getOrElse(InternalServerError), withErrorReport.errorReport)
      case workbenchException: WorkbenchException =>
        val report = ErrorReport(Option(workbenchException.getMessage).getOrElse(""), Some(InternalServerError), Seq(), Seq(), Some(workbenchException.getClass))
        complete(InternalServerError, report)
      case e: IllegalArgumentException => complete(BadRequest, AgoraException(e.getMessage, e.getCause, BadRequest))
      case e: AgoraEntityAuthorizationException => complete(Forbidden, AgoraException(e.getMessage, e.getCause, Forbidden))
      case e: NamespaceAuthorizationException => complete(Forbidden, AgoraException(e.getMessage, e.getCause, Forbidden))
      case e: AgoraEntityNotFoundException => complete(NotFound, AgoraException(e.getMessage, e.getCause, NotFound))
      case e: DockerImageNotFoundException => complete(BadRequest, AgoraException(e.getMessage, e.getCause, BadRequest))
      case e: PermissionNotFoundException => complete(BadRequest, AgoraException(e.getMessage, e.getCause, BadRequest))
      case e: ValidationException => complete(BadRequest, AgoraException(e.getMessage, e.getCause, BadRequest))
      case e: PermissionModificationException => complete(BadRequest, AgoraException(e.getMessage, e.getCause, BadRequest))
      case e: AgoraException => complete(e.statusCode, e)
      case e: Throwable =>
        logger.error("Exception caught by ExceptionHandler: ", e)
        complete(InternalServerError, AgoraException(e.getMessage, e.getCause, InternalServerError))
    }
  }

  val rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder().handle {
      case MalformedRequestContentRejection(message, cause) =>
        complete(BadRequest, AgoraException(message = message, cause, BadRequest))
      case UnacceptedResponseContentTypeRejection(supported) =>
        val msg = s"Resource representation is only available with these Content-Types:\n ${supported.map(_.format).mkString("\n")}"
        complete(NotAcceptable, AgoraException(msg, NotAcceptable))
    }.result()

}

class ApiService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef)
                (implicit
                 ec: ExecutionContext,
                 materializer: Materializer,
                 agoraBusinessExecutionContext: AgoraBusinessExecutionContext
                )
  extends LazyLogging with SwaggerRoutes {

  val statusService = new StatusService(permissionsDataSource, healthMonitor)
  val ga4ghService = new Ga4ghService(permissionsDataSource)
  val methodsService = new MethodsService(permissionsDataSource)
  val configurationsService = new ConfigurationsService(permissionsDataSource)
  val namespacePermissionsService = new NamespacePermissionsService(permissionsDataSource)
  val entityPermissionsService = new EntityPermissionsService(permissionsDataSource)
  val multiEntityPermissionsService = new MultiEntityPermissionsService(permissionsDataSource)

  def route: Route = (logRequestResult & ApiService.handleExceptionsAndRejections) {
    options { complete(OK) } ~ statusService.statusRoute ~ swaggerRoutes ~ ga4ghService.routes ~ methodsService.routes ~
          configurationsService.routes ~ namespacePermissionsService.routes ~ entityPermissionsService.routes ~
          multiEntityPermissionsService.routes
  }

  // basis for logRequestResult lifted from http://stackoverflow.com/questions/32475471/how-does-one-log-akka-http-client-requests
  private def logRequestResult: Directive0 = {
    def entityAsString(entity: HttpEntity): Future[String] = {
      entity.dataBytes
        .map(_.decodeString(entity.contentType.charsetOption.getOrElse(HttpCharsets.`UTF-8`).value))
        .runWith(Sink.head)
    }

    def myLoggingFunction(logger: LoggingAdapter)(req: HttpRequest)(res: Any): Unit = {
      val entry = res match {
        case Complete(resp) =>
          val logLevel: LogLevel = resp.status.intValue / 100 match {
            case 5 => Logging.ErrorLevel
            case _ => Logging.DebugLevel
          }
          entityAsString(resp.entity).map(data => LogEntry(s"${req.method} ${req.uri}: ${resp.status} entity: $data", logLevel))
        case other =>
          Future.successful(LogEntry(s"$other", Logging.DebugLevel)) // I don't really know when this case happens
      }
      entry.map(_.logTo(logger))
    }

    DebuggingDirectives.logRequestResult(LoggingMagnet(log => myLoggingFunction(log)))
  }
}
