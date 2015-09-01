package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * QueryHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for querying the
 * methods repository for methods and method configurations.
 */
class QueryHandler extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness()

  def receive = {
    case QuerySingle(requestContext: RequestContext,
                     entity: AgoraEntity,
                     entityTypes: Seq[AgoraEntityType.EntityType],
                     username: String,
                     onlyPayload: Boolean) =>
      query(requestContext, entity, entityTypes, username, onlyPayload)
      context.stop(self)

    case Query(requestContext: RequestContext,
               agoraSearch: AgoraEntity,
               agoraProjection: Option[AgoraEntityProjection],
               entityTypes: Seq[AgoraEntityType.EntityType],
               username: String) =>
      query(requestContext, agoraSearch, agoraProjection, entityTypes, username)
      context.stop(self)

    case Delete(requestContext: RequestContext,
                entity: AgoraEntity,
                entityTypes: Seq[AgoraEntityType.EntityType],
                username: String) =>
      delete(requestContext, entity, entityTypes, username)
      context.stop(self)
  }

  def query(requestContext: RequestContext,
            entity: AgoraEntity,
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String,
            onlyPayload: Boolean): Unit = {
    val foundEntity = agoraBusiness.findSingle(entity, entityTypes, username: String)
    if (onlyPayload) context.parent ! RequestComplete(foundEntity.payload)
    else context.parent ! RequestComplete(foundEntity)
  }

  def query(requestContext: RequestContext,
            agoraSearch: AgoraEntity,
            agoraProjection: Option[AgoraEntityProjection],
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String): Unit = {
    val entities = agoraBusiness.find(agoraSearch, agoraProjection, entityTypes, username)
    context.parent ! RequestComplete(entities)
  }

  def delete(requestContext: RequestContext,
              entity: AgoraEntity,
              entityTypes: Seq[AgoraEntityType.EntityType],
              username: String): Unit = {
    val rowsDeleted = agoraBusiness.delete(entity, entityTypes, username)
    context.parent ! RequestComplete(rowsDeleted.toString)
  }

}
