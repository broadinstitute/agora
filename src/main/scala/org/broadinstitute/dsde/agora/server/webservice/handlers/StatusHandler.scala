package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.agora.server.model.AgoraStatus
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

class StatusHandler extends Actor {
  implicit val system = context.system

  val agoraStatus = new AgoraDBStatus()
  val loggingEnabled = AgoraConfig.supervisorLogging

  def receive = {
    case ServiceMessages.Status(requestContext: RequestContext) =>
      getStatus(requestContext)
      context.stop(self)
  }

  private def getStatus(requestContext: RequestContext): Unit = {
    val status = agoraStatus.status()
    status match {
      case AgoraStatus(true, messages) => context.parent ! RequestComplete(OK, status)
      case AgoraStatus(false, messages) => context.parent ! RequestComplete(InternalServerError, status)
    }
  }
}