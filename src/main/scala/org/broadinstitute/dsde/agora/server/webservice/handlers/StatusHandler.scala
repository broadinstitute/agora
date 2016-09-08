package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraStatus
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.routing.RequestContext

/**
  * Created by skwalker on 9/6/16.
  */
class StatusHandler extends Actor {
  implicit val system = context.system

  val agoraStatus = new AgoraStatus()
  val loggingEnabled = AgoraConfig.supervisorLogging

  def receive = {
    case ServiceMessages.Status(requestContext: RequestContext) =>
      getStatus(requestContext)
      context.stop(self)
  }

  private def getStatus(requestContext: RequestContext): Unit = {
    context.parent ! RequestComplete(OK, agoraStatus.status())
  }
}