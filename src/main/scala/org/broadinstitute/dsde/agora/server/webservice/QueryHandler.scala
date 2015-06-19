package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.business.{AuthorizationProvider, AgoraBusiness}
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
class QueryHandler(authorizationProvider: AuthorizationProvider) extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness(authorizationProvider)

  def receive = {
    case ServiceMessages.QueryByNamespaceNameSnapshotId(
    requestContext: RequestContext,
    namespace: String,
    name: String,
    snapshotId: Int,
    entityType: Seq[AgoraEntityType.EntityType],
    username: String
    ) =>
      query(requestContext, namespace, name, snapshotId, entityType, username)
      context.stop(self)

    case ServiceMessages.Query(
    requestContext: RequestContext,
    agoraSearch: AgoraEntity,
    agoraProjection: Option[AgoraEntityProjection],
    entityTypes: Seq[AgoraEntityType.EntityType],
    username: String
    ) =>
      query(requestContext, agoraSearch, agoraProjection, entityTypes, username)
      context.stop(self)
  }

  def query(requestContext: RequestContext,
            namespace: String, name: String,
            snapshotId: Int,
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String): Unit = {
    agoraBusiness.findSingle(namespace, name, snapshotId, entityTypes, username: String) match {
      case None => context.parent ! RequestComplete(NotFound, AgoraError(s"Entity: $namespace/$name/$snapshotId not found"))
      case Some(method) => context.parent ! RequestComplete(method)
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
