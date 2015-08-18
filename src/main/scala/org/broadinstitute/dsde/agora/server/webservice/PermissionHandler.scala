package org.broadinstitute.dsde.agora.server.webservice


import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.busines.PermissionBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AccessControl
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.routing.RequestContext


class PermissionHandler extends Actor {
  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  implicit val system = context.system
  val permissionBusiness = new PermissionBusiness()

  def receive = {
    case ListNamespacePermissions(_context: RequestContext, entity: AgoraEntity, requester: String) =>
      val permissions = permissionBusiness.listNamespacePermissions(entity, requester)
      context.parent ! RequestComplete(permissions)
      context.stop(self)

    case InsertNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.insertNamespacePermission(entity, requester, userAccess)
      context.parent ! RequestComplete(rowsChanged.toString)
      context.stop(self)

    case EditNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.editNamespacePermission(entity, requester, userAccess)
      context.parent ! RequestComplete(rowsChanged.toString)
      context.stop(self)

    case DeleteNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userToRemove: String) =>
      val rowsChanged = permissionBusiness.deleteNamespacePermission(entity, requester, userToRemove)
      context.parent ! RequestComplete(rowsChanged.toString)
      context.stop(self)

    case ListEntityPermissions(_context: RequestContext, entity: AgoraEntity, requester: String) =>
      val permissions = permissionBusiness.listEntityPermissions(entity, requester)
      context.parent ! RequestComplete(permissions)
      context.stop(self)

    case InsertEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.insertEntityPermission(entity, requester, userAccess)
      context.parent ! RequestComplete(rowsChanged.toString)
      context.stop(self)

    case EditEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      val rowsChanged = permissionBusiness.editEntityPermission(entity, requester, userAccess)
      context.parent ! RequestComplete(rowsChanged.toString)
      context.stop(self)

    case DeleteEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userToRemove: String) =>
      val rowsChanged = permissionBusiness.deleteEntityPermission(entity, requester, userToRemove)
      context.parent ! RequestComplete(rowsChanged.toString)
      context.stop(self)
  }

}
