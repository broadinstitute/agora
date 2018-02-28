package org.broadinstitute.dsde.agora.server.webservice.handlers

import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}

import scala.concurrent.{ExecutionContext, Future}

/**
 * AddHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for adding a method
 * or method configuration to the methods repository.
 */
class AddHandler(dataSource: PermissionsDataSource)(implicit val ec: ExecutionContext) {
  val permissionBusiness = new PermissionBusiness(dataSource)(ec)
  val agoraBusiness = new AgoraBusiness(dataSource)(ec)

  def add(entity: AgoraEntity, username: String, path: String): Future[AgoraEntity] = {
    val entityWithType = AgoraEntityType.byPath(path) match {
      case AgoraEntityType.Configuration => entity.addEntityType(Option(AgoraEntityType.Configuration))
      case _ => entity
    }
    agoraBusiness.insert(entityWithType, username)
  }
}