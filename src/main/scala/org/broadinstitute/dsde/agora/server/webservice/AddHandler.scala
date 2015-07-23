package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Actor
import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraError}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * AddHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for adding a method
 * or method configuration to the methods repository.
 */
class AddHandler(authorizationProvider: AuthorizationProvider) extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness(authorizationProvider)

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraEntity, username: String) =>
      try {
        add(requestContext, agoraAddRequest, username)
      } catch {
        case e: SyntaxError => context.parent ! RequestComplete(BadRequest, AgoraError("Syntax error in payload: " + e.getMessage))
      }
      context.stop(self)
  }

  private def add(requestContext: RequestContext, agoraEntity: AgoraEntity, username: String): Unit = {
    context.parent ! RequestComplete(Created, agoraBusiness.insert(agoraEntity, username))
  }
}