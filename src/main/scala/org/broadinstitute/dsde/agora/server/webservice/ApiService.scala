package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.event.Logging.LogLevel
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive0, ExceptionHandler, Route}
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.{DebuggingDirectives, LogEntry, LoggingMagnet}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.{AgoraConfig, SwaggerRoutes}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper.Sweep
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AdminSweeper, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.errorReportSource
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraException, ValidationException}
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghService
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.workbench.model.ErrorReportJsonSupport._
import org.broadinstitute.dsde.workbench.model.{ErrorReport, WorkbenchException, WorkbenchExceptionWithErrorReport}

import scala.concurrent.{ExecutionContext, Future}

class ApiService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef)
                (implicit val system: ActorSystem, val ec: ExecutionContext, val materializer: Materializer)
  extends LazyLogging with SwaggerRoutes {

  val statusService = new StatusService(permissionsDataSource, healthMonitor)
  // TODO Instantiate the remaining services once they are converted
  val methodsService = new MethodsService(permissionsDataSource)
  val configurationsService = new ConfigurationsService(permissionsDataSource)
//  val ga4ghService = new Ga4ghService(permissionsDataSource)

  // TODO Add the remaining routes once they are converted
  def route: Route = (logRequestResult & handleExceptions(myExceptionHandler)) {
    options { complete(OK) } ~ statusService.statusRoute ~ methodsService.routes ~
      configurationsService.routes ~ swaggerRoutes
  }

  /**
   * Firecloud system maintains its set of admins as a google group.
   *
   * If such a group is specified in config, poll it at regular intervals
   *   to synchronize the admins defined in our users table
   */
    // TODO Make below work (do we still need it?)
//  AgoraConfig.adminGoogleGroup match {
//    case Some(group) =>
//      import system.dispatcher
//      val adminSweeper = actorRefFactory.actorOf(AdminSweeper.props(AdminSweeper.adminsGoogleGroupPoller, permissionsDataSource))
//      val adminScheduler =
//        context.system.scheduler.schedule(5 seconds, AgoraConfig.adminSweepInterval minutes, adminSweeper, Sweep)
//    case None =>
//  }

  private val myExceptionHandler = {
    ExceptionHandler {
      // TODO Add Agora-specific exceptions if necessary
      case withErrorReport: WorkbenchExceptionWithErrorReport =>
        complete(withErrorReport.errorReport.statusCode.getOrElse(StatusCodes.InternalServerError), withErrorReport.errorReport)
      case workbenchException: WorkbenchException =>
        val report = ErrorReport(Option(workbenchException.getMessage).getOrElse(""), Some(StatusCodes.InternalServerError), Seq(), Seq(), Some(workbenchException.getClass))
        complete(StatusCodes.InternalServerError, report)
      case e: Throwable =>
        //NOTE: this needs SprayJsonSupport._, ErrorReportJsonSupport._, and errorReportSource all imported to work
        complete(StatusCodes.InternalServerError, ErrorReport(e))
    }
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
