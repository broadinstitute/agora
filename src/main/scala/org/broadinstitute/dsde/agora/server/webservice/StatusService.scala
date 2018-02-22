package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
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
import scala.util.{Failure, Success}

abstract class StatusService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef) {
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
          val httpCode = if (status.ok) StatusCodes.OK else StatusCodes.InternalServerError
          complete(httpCode, status)
        case Failure(_) =>
          complete(StatusCodes.InternalServerError, "Unable to gather engine status")
      }
    }
  }
}
