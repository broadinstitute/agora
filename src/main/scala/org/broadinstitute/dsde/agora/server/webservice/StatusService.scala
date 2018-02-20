package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.PerRequest2.{PerRequestMessage, RequestComplete}
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor.GetCurrentStatus
import org.broadinstitute.dsde.workbench.util.health.StatusCheckResponse
import PerRequest2.requestCompleteMarshaller
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

abstract class StatusService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef) {
  implicit def actorRefFactory: ActorRefFactory
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  
  // Derive timeouts (implicit and not) from akka http's request timeout since there's no point in being higher than that
  implicit val duration = ConfigFactory.load().as[FiniteDuration]("akka.http.server.request-timeout")
  implicit val timeout: Timeout = duration

  private def collectStatusInfo(healthMonitor: ActorRef): Future[PerRequestMessage] = {
    import org.broadinstitute.dsde.workbench.util.health.StatusJsonSupport._

    (healthMonitor ? GetCurrentStatus).mapTo[StatusCheckResponse].map { statusCheckResponse =>
      val httpStatus = if (statusCheckResponse.ok) StatusCodes.OK else StatusCodes.InternalServerError
      RequestComplete(httpStatus, statusCheckResponse)
    }
  }

  // GET /status
  def statusRoute: Route = path("status") {
    get {
      complete { collectStatusInfo(healthMonitor) }
    }
  }

}
