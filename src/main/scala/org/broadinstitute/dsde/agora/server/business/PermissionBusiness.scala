package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraEntityPermissionsClient, AgoraPermissions, NamespacePermissionsClient}
import AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityAuthorizationException, AgoraException, NamespaceAuthorizationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import spray.http.StatusCodes
import spray.http.StatusCodes.Conflict


class PermissionBusiness {

  def listNamespacePermissions(entity: AgoraEntity, requester: String): Seq[AccessControl] = {
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.listNamespacePermissions(entity)
  }

  def insertNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.insertNamespacePermission(entity, accessObject)
  }

  def batchNamespacePermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Int = {
    accessObjectList.map(accessObject => insertNamespacePermission(entity, requester, accessObject)).sum
  }

  def editNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    checkSameRequester(requester, accessObject.user)
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.editNamespacePermission(entity, accessObject)
  }

  def deleteNamespacePermission(entity: AgoraEntity, requester: String, userToRemove: String): Int = {
    checkSameRequester(requester, userToRemove)
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.deleteNamespacePermission(entity, userToRemove)
  }

  def listEntityPermissions(entity: AgoraEntity, requester: String): Seq[AccessControl] = {
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.listEntityPermissions(entity)
  }

  def insertEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.insertEntityPermission(entity, accessObject)
  }

  def batchEntityPermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Int = {
    accessObjectList.map(accessObject => insertEntityPermission(entity, requester, accessObject)).sum
  }

  def editEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    checkSameRequester(requester, accessObject.user)
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.editEntityPermission(entity, accessObject)

  }

  def deleteEntityPermission(entity: AgoraEntity, requester: String, userToRemove: String): Int = {
    checkSameRequester(requester, userToRemove)
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.deleteEntityPermission(entity, userToRemove)
  }

  def authorizeNamespaceRequester(entity: AgoraEntity, requester: String): Unit = {
    if (!NamespacePermissionsClient.getNamespacePermission(entity, requester).canManage &&
        !NamespacePermissionsClient.getNamespacePermission(entity, "public").canManage)
      throw NamespaceAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  def authorizeEntityRequester(entity: AgoraEntity, requester: String): Unit = {
    if (!AgoraEntityPermissionsClient.getEntityPermission(entity, requester).canManage &&
        !AgoraEntityPermissionsClient.getEntityPermission(entity, "public").canManage)
      throw AgoraEntityAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  def checkSameRequester(requester: String, userToModify: String):Unit = {
    if (requester.equalsIgnoreCase(userToModify)) {
      val m = "Unable to modify access control for current user"
      throw AgoraException(m, new Exception(m), Conflict)
    }
  }

}