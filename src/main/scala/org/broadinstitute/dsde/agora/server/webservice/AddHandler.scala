package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Actor
import cromwell.binding.WdlNamespace
import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.business.{AuthorizationProvider, AgoraBusiness}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, AgoraError}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * Handles adding a method to the methods repository, including validation.
 */
class AddHandler(authorizationProvider: AuthorizationProvider) extends Actor {

  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness()

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraEntity, username: String) =>
      try {
        validatePayload(agoraAddRequest)
        add(requestContext, agoraAddRequest, username)
      } catch {
        case e: SyntaxError => context.parent ! RequestComplete(BadRequest, AgoraError("Syntax error in payload: " + e.getMessage))
      }
      context.stop(self)
  }

  private def validatePayload(agoraEntity: AgoraEntity): Unit = {
    agoraEntity.entityType.get match {
      case AgoraEntityType.Task =>
        WdlNamespace.load(agoraEntity.payload.get)
      case AgoraEntityType.Workflow =>
        WdlNamespace.load(agoraEntity.payload.get)
      case AgoraEntityType.Configuration =>
      //add config validation here
    }
  }

  private def add(requestContext: RequestContext, agoraEntity: AgoraEntity, username: String): Unit = {
    val authorizedEntity = authorizationProvider.authorizationsForEntity(Some(agoraEntity), username)
    if (!authorizedEntity.authorization.canCreate) {
      context.parent ! RequestComplete(BadRequest, AgoraError("You don't have permission to create items in " + agoraEntity.namespace))
    }
    val method = agoraBusiness.insert(agoraEntity.copy(createDate = Option(new DateTime())), username)
    context.parent ! RequestComplete(spray.http.StatusCodes.Created.intValue, method)
  }
}
