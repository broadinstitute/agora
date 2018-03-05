package org.broadinstitute.dsde.agora.server.webservice.handlers

import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

import scala.concurrent.{ExecutionContext, Future}

/**
 * QueryHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for querying the
 * methods repository for methods and method configurations.
 */
class QueryHandler(dataSource: PermissionsDataSource)(implicit ec: ExecutionContext) {
  val agoraBusiness = new AgoraBusiness(dataSource)(ec)
  val permissionBusiness = new PermissionBusiness(dataSource)(ec)

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
}
