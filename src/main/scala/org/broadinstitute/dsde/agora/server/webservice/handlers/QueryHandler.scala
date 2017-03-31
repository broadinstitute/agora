package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import akka.pattern._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

import scala.concurrent.{ExecutionContext, Future}

/**
 * QueryHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for querying the
 * methods repository for methods and method configurations.
 */
class QueryHandler(dataSource: PermissionsDataSource)(implicit ec: ExecutionContext) extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness(dataSource)(ec)
  val permissionBusiness = new PermissionBusiness(dataSource)(ec)

  def receive = {
    case QuerySingle(requestContext: RequestContext,
                     entity: AgoraEntity,
                     entityTypes: Seq[AgoraEntityType.EntityType],
                     username: String,
                     onlyPayload: Boolean) =>
      query(requestContext, entity, entityTypes, username, onlyPayload) pipeTo context.parent

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
            onlyPayload: Boolean): Future[PerRequestMessage] = {
    permissionBusiness.addUserIfNotInDatabase(username) flatMap { _ =>
      agoraBusiness.findSingle(entity, entityTypes, username: String) map { foundEntity =>
        if (onlyPayload) RequestComplete(foundEntity.payload)
        else RequestComplete(foundEntity)
      }
    }
  }

  def query(requestContext: RequestContext,
            agoraSearch: AgoraEntity,
            agoraProjection: Option[AgoraEntityProjection],
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String): Future[PerRequestMessage] = {
    permissionBusiness.addUserIfNotInDatabase(username) flatMap { _ =>
      agoraBusiness.find(agoraSearch, agoraProjection, entityTypes, username) map { entities =>
        RequestComplete(entities)
      }
    }
  }

  def delete(requestContext: RequestContext,
              entity: AgoraEntity,
              entityTypes: Seq[AgoraEntityType.EntityType],
              username: String): Future[PerRequestMessage] = {
    permissionBusiness.addUserIfNotInDatabase(username) flatMap { _ =>
      agoraBusiness.delete(entity, entityTypes, username) map { rowsDeleted =>
        RequestComplete(rowsDeleted.toString)
      }
    }
  }

}
