package org.broadinstitute.dsde.agora.server.webservice

import java.util.concurrent.TimeoutException

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AuthorizationProvider, ClientServiceFailure}
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType, AgoraError}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * Actor responsible for querying the methods repository.
 */
class QueryHandler(authorizationProvider: AuthorizationProvider) extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness()

  def receive = {
    case QuerySingle(requestContext: RequestContext,
                     namespace: String,
                     name: String,
                     snapshotId: Int,
                     entityType: Seq[AgoraEntityType.EntityType],
                     username: String,
                     onlyPayload: Boolean) =>
      query(requestContext, namespace, name, snapshotId, entityType, username, onlyPayload)
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
            namespace: String, name: String,
            snapshotId: Int,
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String,
            onlyPayload: Boolean): Unit = {
    val entity = agoraBusiness.findSingle(namespace, name, snapshotId, entityTypes, username: String)
    entity match {
      case Some(agoraEntity) =>
        if (authorizationProvider.isAuthorizedForRead(agoraEntity, username)) {
          if (onlyPayload) context.parent ! RequestComplete(agoraEntity.payload)
          else context.parent ! RequestComplete(agoraEntity)
        }
        else context.parent ! RequestComplete(NotFound, AgoraError(s"Entity: $namespace/$name/$snapshotId not found"))
      case None => context.parent ! RequestComplete(NotFound, AgoraError(s"Entity: $namespace/$name/$snapshotId not found"))
    }
  }

  def query(requestContext: RequestContext,
            agoraSearch: AgoraEntity,
            agoraProjection: Option[AgoraEntityProjection],
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String): Unit = {
    val entities = agoraBusiness.find(agoraSearch, agoraProjection, entityTypes, username)
      context.parent ! RequestComplete(authorizationProvider.filterByReadPermissions(entities, username))
  }

}
