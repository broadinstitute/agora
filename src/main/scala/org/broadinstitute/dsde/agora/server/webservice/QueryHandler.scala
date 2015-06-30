package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType, AgoraError}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * Actor responsible for querying the methods repository.
 */
class QueryHandler extends Actor {
  implicit val system = context.system

  def receive = {
    case ServiceMessages.QuerySingle(
          requestContext: RequestContext,
          namespace: String,
          name: String,
          snapshotId: Int,
          entityType: Seq[AgoraEntityType.EntityType],
          onlyPayload: Boolean
    ) =>
      query(requestContext, namespace, name, snapshotId, entityType, onlyPayload)
      context.stop(self)

    case ServiceMessages.Query(
          requestContext: RequestContext,
          agoraSearch: AgoraEntity,
          agoraProjection: Option[AgoraEntityProjection],
          entityTypes: Seq[AgoraEntityType.EntityType]
    ) =>
      query(requestContext, agoraSearch, agoraProjection, entityTypes)
      context.stop(self)
  }

  def query(requestContext: RequestContext,
            namespace: String, name: String,
            snapshotId: Int,
            entityTypes: Seq[AgoraEntityType.EntityType],
            onlyPayload: Boolean): Unit = {

    AgoraBusiness.findSingle(namespace, name, snapshotId, entityTypes) match {
      case None => context.parent ! RequestComplete(NotFound,
                                                    AgoraError(s"Entity: $namespace/$name/$snapshotId not found"))
      case Some(method) =>
        if (onlyPayload) {context.parent ! RequestComplete(method.payload)}
        else {context.parent ! RequestComplete(method)}
    }

  }

  def query(requestContext: RequestContext,
            agoraSearch: AgoraEntity,
            agoraProjection: Option[AgoraEntityProjection],
            entityTypes: Seq[AgoraEntityType.EntityType]): Unit = {
    val entities = AgoraBusiness.find(agoraSearch, agoraProjection, entityTypes)
    context.parent ! RequestComplete(entities)
  }

}
