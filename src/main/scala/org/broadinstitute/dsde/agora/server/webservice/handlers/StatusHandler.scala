package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import akka.pattern._
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.AgoraStatus
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.{PerRequestMessage, RequestComplete}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

import scala.concurrent.{ExecutionContext, Future}

class StatusHandler(dataSource: PermissionsDataSource, implicit val ec: ExecutionContext) extends Actor {
  implicit val system = context.system

  val agoraStatus = new AgoraDBStatus(dataSource)(ec)
  val loggingEnabled = AgoraConfig.supervisorLogging

  def receive = {
    case ServiceMessages.Status(requestContext: RequestContext) =>
      getStatus(requestContext) pipeTo context.parent
  }

  private def getStatus(requestContext: RequestContext): Future[PerRequestMessage] = {
    val status = agoraStatus.status()
    status map {
      case AgoraStatus(true, messages) => RequestComplete(OK, status)
      case AgoraStatus(false, messages) => RequestComplete(InternalServerError, status)
    }
  }
}
