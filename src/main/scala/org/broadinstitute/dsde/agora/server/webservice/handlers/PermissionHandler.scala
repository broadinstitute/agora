package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import akka.pattern._
import org.broadinstitute.dsde.agora.server.business.PermissionBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions, EntityAccessControl, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.routing.RequestContext

import scala.concurrent.ExecutionContext


class PermissionHandler(dataSource: PermissionsDataSource, implicit val ec: ExecutionContext) extends Actor {

  // JSON Serialization Support
  import spray.httpx.SprayJsonSupport._
  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._

  implicit val system = context.system
  val permissionBusiness = new PermissionBusiness(dataSource)

  def receive = {
    case ListNamespacePermissions(_context: RequestContext, entity: AgoraEntity, requester: String) =>
      (permissionBusiness.listNamespacePermissions(entity, requester) map { permissions =>
        RequestComplete(permissions)
      }) pipeTo context.parent

    case InsertNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      (permissionBusiness.insertNamespacePermission(entity, requester, userAccess) map { rowsChanged =>
        RequestComplete(userAccess)
      }) pipeTo context.parent

    case BatchNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccessList: List[AccessControl]) =>
      (permissionBusiness.batchNamespacePermission(entity, requester, userAccessList) map { rowsChanged =>
        RequestComplete(userAccessList)
      }) pipeTo context.parent

    case EditNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      (permissionBusiness.editNamespacePermission(entity, requester, userAccess) map { rowsChanged =>
        RequestComplete(userAccess)
      }) pipeTo context.parent

    case DeleteNamespacePermission(_context: RequestContext, entity: AgoraEntity, requester: String, userToRemove: String) =>
      (permissionBusiness.deleteNamespacePermission(entity, requester, userToRemove) map { rowsChanged =>
        RequestComplete(AccessControl(userToRemove, AgoraPermissions(Nothing)))
      }) pipeTo context.parent

    case ListEntityPermissions(_context: RequestContext, entity: AgoraEntity, requester: String) =>
      (permissionBusiness.listEntityPermissions(entity, requester) map { permissions =>
        RequestComplete(permissions)
      }) pipeTo context.parent

    case ListMultiEntityPermissions(_context: RequestContext, entities: List[AgoraEntity], requester: String) =>
      (permissionBusiness.listEntityPermissions(entities, requester) map { permissions =>
        RequestComplete(permissions)
      }) pipeTo context.parent

    case UpsertMultiEntityPermissions(_context: RequestContext, aclPairs: List[EntityAccessControl], requester: String) =>
      (permissionBusiness.upsertEntityPermissions(aclPairs, requester) map { permissions =>
        RequestComplete(permissions)
      }) pipeTo context.parent

    case InsertEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      (permissionBusiness.insertEntityPermission(entity, requester, userAccess) map { rowsChanged =>
        RequestComplete(userAccess)
      }) pipeTo context.parent

    case BatchEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccessList: List[AccessControl]) =>
      (permissionBusiness.batchEntityPermission(entity, requester, userAccessList) map {rowsChanged =>
        RequestComplete(userAccessList)
      }) pipeTo context.parent

    case EditEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userAccess: AccessControl) =>
      (permissionBusiness.editEntityPermission(entity, requester, userAccess) map { rowsChanged =>
        RequestComplete(userAccess)
      }) pipeTo context.parent

    case DeleteEntityPermission(_context: RequestContext, entity: AgoraEntity, requester: String, userToRemove: String) =>
      (permissionBusiness.deleteEntityPermission(entity, requester, userToRemove) map { rowsChanged =>
        RequestComplete(AccessControl(userToRemove, AgoraPermissions(Nothing)))
      }) pipeTo context.parent
  }

}
