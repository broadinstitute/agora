package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging.LogLevel
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, ExceptionHandler, Route}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.SwaggerRoutes
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.exceptions._
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghService
import org.broadinstitute.dsde.workbench.model.{ErrorReport, WorkbenchException, WorkbenchExceptionWithErrorReport}

import scala.concurrent.{ExecutionContext, Future}

object ApiService {

  // Required for marshalling
  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import org.broadinstitute.dsde.workbench.model.ErrorReportJsonSupport._
  import org.broadinstitute.dsde.agora.server.errorReportSource

  val exceptionHandler: ExceptionHandler = {
    ExceptionHandler {
      case withErrorReport: WorkbenchExceptionWithErrorReport =>
        complete(withErrorReport.errorReport.statusCode.getOrElse(StatusCodes.InternalServerError), withErrorReport.errorReport)
      case workbenchException: WorkbenchException =>
        val report = ErrorReport(Option(workbenchException.getMessage).getOrElse(""), Some(StatusCodes.InternalServerError), Seq(), Seq(), Some(workbenchException.getClass))
        complete(StatusCodes.InternalServerError, report)
      case e: IllegalArgumentException => complete(BadRequest, ErrorReport(e))
      case e: AgoraEntityAuthorizationException => complete(Forbidden, ErrorReport(e))
      case e: NamespaceAuthorizationException => complete(Forbidden, ErrorReport(e))
      case e: AgoraEntityNotFoundException => complete(NotFound, ErrorReport(e))
      case e: DockerImageNotFoundException => complete(BadRequest, ErrorReport(e))
      case e: PermissionNotFoundException => complete(BadRequest, ErrorReport(e))
      case e: ValidationException => complete(BadRequest, ErrorReport(e))
      case e: wdl.exception.ValidationException => complete(BadRequest, ErrorReport(e))
      case e: PermissionModificationException => complete(BadRequest, ErrorReport(e))
      case e: AgoraException => complete(StatusCodes.getForKey(e.statusCode.intValue).getOrElse(StatusCodes.InternalServerError), ErrorReport(e))
      case e: Throwable => complete(StatusCodes.InternalServerError, ErrorReport(e))
    }
  }
}

class ApiService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef)
                (implicit val system: ActorSystem, val ec: ExecutionContext, val materializer: Materializer)
  extends LazyLogging with SwaggerRoutes {

  val statusService = new StatusService(permissionsDataSource, healthMonitor)
  val ga4ghService = new Ga4ghService(permissionsDataSource)
  // TODO Instantiate the remaining services once they are converted
  //  val methodsService = new MethodsService(permissionsDataSource)
  //  val configurationsService = new ConfigurationsService(permissionsDataSource)

  // TODO Add the remaining routes once they are converted
  def route: Route = (logRequestResult & handleExceptions(ApiService.exceptionHandler)) {
    options { complete(OK) } ~ statusService.statusRoute ~ swaggerRoutes ~ ga4ghService.routes
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
