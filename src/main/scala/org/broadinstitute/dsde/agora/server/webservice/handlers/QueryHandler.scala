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
import spray.http.StatusCodes.BadRequest

import scala.concurrent.{ExecutionContext, Future}

/**
 * QueryHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for querying the
 * methods repository for methods and method configurations.
 */
class QueryHandler(dataSource: PermissionsDataSource, implicit val ec: ExecutionContext) extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness(dataSource)(ec)
  val permissionBusiness = new PermissionBusiness(dataSource)(ec)

  def receive = {
    case QuerySingle(requestContext: RequestContext,
                     entity: AgoraEntity,
                     entityTypes: Seq[AgoraEntityType.EntityType],
                     username: String,
                     onlyPayload: Boolean,
                     payloadAsObject: Boolean) =>
      query(requestContext, entity, entityTypes, username, onlyPayload, payloadAsObject) pipeTo context.parent

    case Query(requestContext: RequestContext,
               agoraSearch: AgoraEntity,
               agoraProjection: Option[AgoraEntityProjection],
               entityTypes: Seq[AgoraEntityType.EntityType],
               username: String) =>
      query(requestContext, agoraSearch, agoraProjection, entityTypes, username) pipeTo context.parent

    case QueryDefinitions(requestContext: RequestContext,
                          username: String) =>
      queryDefinitions(requestContext, username) pipeTo context.parent

    case QueryAssociatedConfigurations(requestContext: RequestContext,
                                       namespace: String,
                                       name: String,
                                       username: String) =>
      queryAssociatedConfigurations(requestContext, namespace, name, username) pipeTo context.parent

    case QueryCompatibleConfigurations(requestContext: RequestContext,
                                       namespace: String,
                                       name: String,
                                       snapshotId: Int,
                                       username: String) =>
      queryCompatibleConfigurations(requestContext, namespace, name, snapshotId, username) pipeTo context.parent


    case Delete(requestContext: RequestContext,
                entity: AgoraEntity,
                entityTypes: Seq[AgoraEntityType.EntityType],
                username: String) =>
      delete(requestContext, entity, entityTypes, username) pipeTo context.parent

    case Copy(requestContext: RequestContext,
              oldEntity: AgoraEntity,
              newEntity: AgoraEntity,
              redact: Boolean,
              entityTypes: Seq[AgoraEntityType.EntityType],
              username: String) =>
      copy(requestContext, oldEntity, newEntity, redact, entityTypes, username) pipeTo context.parent
  }

  def query(requestContext: RequestContext,
            entity: AgoraEntity,
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String,
            onlyPayload: Boolean,
            payloadAsObject: Boolean): Future[PerRequestMessage] = {
    agoraBusiness.findSingle(entity, entityTypes, username: String) map { foundEntity =>

      (onlyPayload, payloadAsObject) match {
        case (true, true) => RequestComplete(BadRequest, "onlyPayload, payloadAsObject cannot be used together")
        case (true, false) => RequestComplete(foundEntity.payload)
        case (false, true) => RequestComplete(foundEntity.withDeserializedPayload)
        case _ => RequestComplete(foundEntity)
      }

    }
  }

  def query(requestContext: RequestContext,
            agoraSearch: AgoraEntity,
            agoraProjection: Option[AgoraEntityProjection],
            entityTypes: Seq[AgoraEntityType.EntityType],
            username: String): Future[PerRequestMessage] = {
    agoraBusiness.find(agoraSearch, agoraProjection, entityTypes, username) map { entities =>
      RequestComplete(entities)
    }
  }

  def queryDefinitions(requestContext: RequestContext,
                       username: String): Future[PerRequestMessage] = {
    agoraBusiness.listDefinitions(username) map { definitions =>
      RequestComplete(definitions)
    }
  }

  def queryAssociatedConfigurations(requestContext: RequestContext,
                                    namespace: String,
                                    name: String,
                                    username: String): Future[PerRequestMessage] = {
    agoraBusiness.listAssociatedConfigurations(namespace, name, username) map { configs =>
      RequestComplete(configs)
    }
  }
  def queryCompatibleConfigurations(requestContext: RequestContext,
                                    namespace: String,
                                    name: String,
                                    snapshotId: Int,
                                    username: String): Future[PerRequestMessage] = {
    agoraBusiness.listCompatibleConfigurations(namespace, name, snapshotId, username) map { configs =>
      RequestComplete(configs)
    }
  }
  def delete(requestContext: RequestContext,
              entity: AgoraEntity,
              entityTypes: Seq[AgoraEntityType.EntityType],
              username: String): Future[PerRequestMessage] = {
    agoraBusiness.delete(entity, entityTypes, username) map { rowsDeleted =>
      RequestComplete(rowsDeleted.toString)
    }
  }

  def copy(requestContext: RequestContext,
             oldEntity: AgoraEntity,
             newEntity: AgoraEntity,
             redact: Boolean,
             entityTypes: Seq[AgoraEntityType.EntityType],
             username: String): Future[PerRequestMessage] = {
    agoraBusiness.copy(oldEntity, newEntity, redact, entityTypes, username) map { entities =>
      RequestComplete(entities)
    }
  }

}
