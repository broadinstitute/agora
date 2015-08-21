package org.broadinstitute.dsde.agora.server.busines

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AgoraEntityPermissionsClient, NamespacePermissionsClient, AccessControl, AgoraPermissions}
import AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.exceptions.{NamespaceAuthorizationException, AgoraEntityAuthorizationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity


class PermissionBusiness {

  def listNamespacePermissions(entity: AgoraEntity, requester: String) = {
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.listNamespacePermissions(entity)
  }

  def insertNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl) = {
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.insertNamespacePermission(entity, accessObject)
  }

  def editNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl) = {
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.editNamespacePermission(entity, accessObject)
  }

  def deleteNamespacePermission(entity: AgoraEntity, requester: String, userToRemove: String) = {
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.deleteNamespacePermission(entity, userToRemove)
  }

  def listEntityPermissions(entity: AgoraEntity, requester: String) = {
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.listEntityPermissions(entity)
  }

  def insertEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl) = {
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.insertEntityPermission(entity, accessObject)

  }

  def editEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl) = {
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.editEntityPermission(entity, accessObject)

  }

  def deleteEntityPermission(entity: AgoraEntity, requester: String, userToRemove: String) = {
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.deleteEntityPermission(entity, userToRemove)
  }

  def authorizeNamespaceRequester(entity: AgoraEntity, requester: String) = {
    if (!NamespacePermissionsClient.getNamespacePermission(entity, requester).canManage)
      throw new NamespaceAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  def authorizeEntityRequester(entity: AgoraEntity, requester: String) = {
    if (!AgoraEntityPermissionsClient.getEntityPermission(entity, requester).canManage)
      throw new AgoraEntityAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }
}