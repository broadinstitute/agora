package org.broadinstitute.dsde.agora.server.webservice.methods

import akka.actor.Actor
import cromwell.binding.WdlBinding
import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * Handles adding a method to the methods repository, including validation.
 */
class MethodsAddHandler extends Actor {

  implicit val system = context.system

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraEntity) =>
      try {
        validatePayload(agoraAddRequest)
        add(requestContext, agoraAddRequest)
      } catch {
        case e: SyntaxError => context.parent ! RequestComplete(BadRequest, "Syntax error in payload: " + e.getMessage)
      }
      context.stop(self)
  }

  private def validatePayload(agoraEntity: AgoraEntity): Unit = {
    WdlBinding.getAst(agoraEntity.payload.get, agoraEntity.name.get)
  }

  private def add(requestContext: RequestContext, agoraEntity: AgoraEntity): Unit = {
    val method = AgoraDao.createAgoraDao.insert(agoraEntity)
    context.parent ! RequestComplete(method)
  }
}
