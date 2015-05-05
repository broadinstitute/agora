package org.broadinstitute.dsde.agora.server.webservice.methods

import java.util.Date
import akka.actor.Actor
import com.novus.salat._
import com.novus.salat.global._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.{AgoraAddRequest, AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import cromwell.binding.WdlBinding
import cromwell.parser.WdlParser.SyntaxError
import spray.http.StatusCodes._
import spray.routing.RequestContext

/**
 * Handles adding a method to the methods repository, including validation.
 */
class MethodsAddHandler extends Actor {

  implicit val system = context.system

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraAddRequest) =>
      try {
        validatePayload(agoraAddRequest)
        add(requestContext, agoraAddRequest)
      } catch {
        case e: SyntaxError => context.parent ! RequestComplete(BadRequest, "Syntax error in payload: " + e.getMessage())
      }
      context.stop(self)
  }

  private def validatePayload(agoraAddRequest: AgoraAddRequest): Unit = {
    WdlBinding.getAst(agoraAddRequest.payload, agoraAddRequest.name)
  }

  private def add(requestContext: RequestContext, agoraAddRequest: AgoraAddRequest): Unit = {
    val agoraEntity = AgoraEntity.fromAgoraAddRequest(agoraAddRequest, createDate = Option(new Date())) 
    val method = AgoraDao.createAgoraDao.insert(agoraEntity)
    context.parent ! grater[AgoraEntity].toJSON(method)
  }
}
