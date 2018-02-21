package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor.GetCurrentStatus
import org.broadinstitute.dsde.workbench.util.health.StatusCheckResponse
import org.broadinstitute.dsde.workbench.util.health.StatusJsonSupport._

import scala.concurrent.duration.FiniteDuration
import scala.util.Success

abstract class StatusService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef) {
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  // Derive timeouts (implicit and not) from akka http's request timeout since there's no point in being higher than that
  implicit val duration = ConfigFactory.load().as[FiniteDuration]("akka.http.server.request-timeout")
  implicit val timeout: Timeout = duration

  private val failedStatusAttempt = StatusCheckResponse(ok = false, systems = Map.empty)

  // GET /status
  def statusRoute: Route = path("status") {
    def completeWith(statusCode: StatusCode, statusResponse: StatusCheckResponse) =
      complete {
        HttpResponse.apply(
          statusCode,
          List[HttpHeader](),
          HttpEntity.apply(
            ContentTypes.`application/json`,
            StatusCheckResponseFormat.write(statusResponse).toString()),
          HttpProtocols.`HTTP/1.1`)
      }

    get {
      val statusAttempt = (healthMonitor ? GetCurrentStatus).mapTo[StatusCheckResponse]

      onComplete(statusAttempt) {
        case Success(status) if status.ok => completeWith(StatusCodes.OK, status)
        case status => completeWith(StatusCodes.InternalServerError, status.getOrElse(failedStatusAttempt))
      }
    }
  }
}
