package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor.GetCurrentStatus
import org.broadinstitute.dsde.workbench.util.health.{StatusCheckResponse, SubsystemStatus, Subsystems}
import org.broadinstitute.dsde.workbench.util.health.StatusJsonSupport._

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class StatusService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef) {
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  // Derive timeouts (implicit and not) from akka http's request timeout since there's no point in being higher than that
  implicit val duration = ConfigFactory.load().as[FiniteDuration]("akka.http.server.request-timeout")
  implicit val timeout: Timeout = duration

  // GET /status
  def statusRoute: Route = path("status") {
    get {
      val statusAttempt = (healthMonitor ? GetCurrentStatus).mapTo[StatusCheckResponse]

      onComplete(statusAttempt) {
        case Success(status) =>
          val httpCode = if (status.ok) OK else InternalServerError
          complete(httpCode, status)
        case Failure(_) =>
          val agoraStatus = SubsystemStatus(ok = false, Some(List("Unable to gather subsystem status")))
          complete(InternalServerError, StatusCheckResponse(ok = false, Map(Subsystems.Agora -> agoraStatus)))
      }
    }
  }
}
