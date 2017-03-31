package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import akka.pattern._
import org.broadinstitute.dsde.agora.server.business.PermissionBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext


class PermissionHandler(dataSource: PermissionsDataSource)(implicit ec: ExecutionContext) extends Actor {

  // JSON Serialization Support
  import spray.httpx.SprayJsonSupport._
  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._

  implicit val system = context.system
  val permissionBusiness = new PermissionBusiness(dataSource)(ec)

  def receive = {
    case ListNamespacePermissions(_context: RequestContext, entity: AgoraEntity, requester: String) =>
      (permissionBusiness.listNamespacePermissions(entity, requester) map { permissions =>
        RequestComplete(permissions)
      }) pipeTo context.parent

    case InsertNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.insertNamespacePermission(entity, requester, userAccess)
      context.parent ! RequestComplete(userAccess)
      context.stop(self)

    case BatchNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccessList: List[AccessControl]) =>
      val rowsChanged = permissionBusiness.batchNamespacePermission(entity, requester, userAccessList)
      context.parent ! RequestComplete(userAccessList)
      context.stop(self)

    case EditNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.editNamespacePermission(entity, requester, userAccess)
      context.parent ! RequestComplete(userAccess)
      context.stop(self)

    case DeleteNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userToRemove: String) =>
      val rowsChanged = permissionBusiness.deleteNamespacePermission(entity, requester, userToRemove)
      context.parent ! RequestComplete(AccessControl(userToRemove, AgoraPermissions(Nothing)))
      context.stop(self)

    case ListEntityPermissions(_context: RequestContext, entity: AgoraEntity, requester: String) =>
      val permissions = permissionBusiness.listEntityPermissions(entity, requester)
      context.parent ! RequestComplete(permissions)
      context.stop(self)

    case InsertEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.insertEntityPermission(entity, requester, userAccess)
      context.parent ! RequestComplete(userAccess)
      context.stop(self)

    case BatchEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccessList: List[AccessControl]) =>
      val rowsChanged = permissionBusiness.batchEntityPermission(entity, requester, userAccessList)
      context.parent ! RequestComplete(userAccessList)
      context.stop(self)

    case EditEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.editEntityPermission(entity, requester, userAccess)
      context.parent ! RequestComplete(userAccess)
      context.stop(self)

    case DeleteEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userToRemove: String) =>
      val rowsChanged = permissionBusiness.deleteEntityPermission(entity, requester, userToRemove)
      context.parent ! RequestComplete(AccessControl(userToRemove, AgoraPermissions(Nothing)))
      context.stop(self)
  }

}
