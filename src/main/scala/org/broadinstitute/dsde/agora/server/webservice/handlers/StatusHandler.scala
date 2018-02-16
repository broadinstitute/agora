package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.{Actor, ActorRef}
import akka.http.scaladsl.model.StatusCodes
import akka.pattern._
import akka.util.Timeout
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.PerRequest2.{PerRequestMessage, RequestComplete}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor.GetCurrentStatus
import org.broadinstitute.dsde.workbench.util.health.StatusCheckResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class StatusHandler(dataSource: PermissionsDataSource, implicit val ec: ExecutionContext) extends Actor {
  implicit val system = context.system
  implicit val timeout = Timeout(1.minute) // timeout for the ask to healthMonitor for GetCurrentStatus

  def receive = {
    case ServiceMessages.Status(healthMonitor: ActorRef) =>
      collectStatusInfo(healthMonitor) pipeTo context.parent
  }

  private def collectStatusInfo(healthMonitor: ActorRef): Future[PerRequestMessage] = {
    (healthMonitor ? GetCurrentStatus).mapTo[StatusCheckResponse].map { statusCheckResponse =>
      val httpStatus = if (statusCheckResponse.ok) StatusCodes.OK else StatusCodes.InternalServerError
      RequestComplete(httpStatus)
    }
  }
}
