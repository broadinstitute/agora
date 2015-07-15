package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType, AgoraError}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * QueryHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for querying the
 * methods repository for methods and method configurations.
 */
class QueryHandler(authorizationProvider: AuthorizationProvider) extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness(authorizationProvider)

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
  }

  def query(requestContext: RequestContext,
            entity: AgoraEntity,
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String,
            onlyPayload: Boolean): Unit = {
    val foundEntity = agoraBusiness.findSingle(entity, entityTypes, username: String)
    foundEntity match {
      case Some(agoraEntity) =>
        if (onlyPayload) context.parent ! RequestComplete(agoraEntity.payload)
        else context.parent ! RequestComplete(agoraEntity)
      case None => context.parent ! RequestComplete(NotFound, AgoraError(s"Entity: $entity not found"))
    }
  }

  def query(requestContext: RequestContext,
            agoraSearch: AgoraEntity,
            agoraProjection: Option[AgoraEntityProjection],
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String): Unit = {
    val entities = agoraBusiness.find(agoraSearch, agoraProjection, entityTypes, username)
    context.parent ! RequestComplete(entities)
  }

}
