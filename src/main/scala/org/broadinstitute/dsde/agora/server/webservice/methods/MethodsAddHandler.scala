package org.broadinstitute.dsde.agora.server.webservice.methods

import akka.actor.Actor
import cromwell.binding.WdlBinding
import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraError, AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.joda.time.DateTime
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
        case e: SyntaxError => context.parent ! RequestComplete(BadRequest, AgoraError("Syntax error in payload: " + e.getMessage))
      }
      context.stop(self)
  }

  private def validatePayload(agoraEntity: AgoraEntity): Unit = {
    WdlBinding.getAst(agoraEntity.payload.get, agoraEntity.name.get)
//    WdlBinding.process(agoraEntity.payload.get, agoraEntity.name.get, AgoraBusiness.importResolver)
  }

  private def add(requestContext: RequestContext, agoraEntity: AgoraEntity): Unit = {
    val method = AgoraBusiness.insert(agoraEntity.copy(createDate = Option(new DateTime())))
    context.parent ! RequestComplete(spray.http.StatusCodes.Created.intValue, method)
  }
}
