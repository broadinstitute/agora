package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraEntityPermissionsClient, AgoraPermissions, NamespacePermissionsClient}
import AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityAuthorizationException, PermissionModificationException, NamespaceAuthorizationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

class PermissionBusiness {

  def listNamespacePermissions(entity: AgoraEntity, requester: String): Seq[AccessControl] = {
    authorizeNamespaceRequester(entity, requester)
    NamespacePermissionsClient.listNamespacePermissions(entity)
  }

  def insertNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    authorizeNamespaceRequester(entity, requester, accessObject)
    NamespacePermissionsClient.insertNamespacePermission(entity, accessObject)
  }

  def batchNamespacePermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Int = {
    // Batch authorize, then batch insert.
    accessObjectList.foreach(accessObject => authorizeNamespaceRequester(entity, requester, accessObject))
    accessObjectList.map(accessObject => NamespacePermissionsClient.insertNamespacePermission(entity, accessObject)).sum
  }

  def editNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    authorizeNamespaceRequester(entity, requester, accessObject)
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
    authorizeEntityRequester(entity, requester, accessObject)
    AgoraEntityPermissionsClient.insertEntityPermission(entity, accessObject)
  }

  def batchEntityPermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Int = {
    // Batch authorize, then batch insert.
    accessObjectList.foreach(accessObject => authorizeEntityRequester(entity, requester, accessObject))
    accessObjectList.map(accessObject => AgoraEntityPermissionsClient.insertEntityPermission(entity, accessObject)).sum
  }

  def editEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    authorizeEntityRequester(entity, requester, accessObject)
    AgoraEntityPermissionsClient.editEntityPermission(entity, accessObject)

  }

  def deleteEntityPermission(entity: AgoraEntity, requester: String, userToRemove: String): Int = {
    checkSameRequester(requester, userToRemove)
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.deleteEntityPermission(entity, userToRemove)
  }


  /**
    * Utility method to both authorize the namespace requester and check that the requester is not modifying their own
    * permissions
    */
  private def authorizeNamespaceRequester(entity: AgoraEntity, requester: String, accessObject: AccessControl): Unit = {
    val namespaceACLs = getNamespaceACLs(entity, requester)
    checkSameRequesterAndPermissions(namespaceACLs.find(acl => acl.user.equals(requester)), accessObject)
    if (!namespaceACLs.exists(_.roles.canManage))
      throw NamespaceAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  private def authorizeNamespaceRequester(entity: AgoraEntity, requester: String): Unit = {
    authorizeNamespaceRequester(getNamespaceACLs(entity, requester), entity, requester)
  }

  private def authorizeNamespaceRequester(acls: Seq[AccessControl], entity: AgoraEntity, requester: String): Unit = {
    if (!acls.exists(_.roles.canManage))
      throw NamespaceAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  /**
    * Utility method to both authorize the entity requester and check that the requester is not modifying their own
    * permissions
    */
  private def authorizeEntityRequester(entity: AgoraEntity, requester: String, accessObject: AccessControl): Unit = {
    val currentACLs = getEntityACLs(entity, requester)
    checkSameRequesterAndPermissions(currentACLs.find(acl => acl.user.equals(requester)), accessObject)
    authorizeEntityRequester(currentACLs, entity, requester)
  }

  private def authorizeEntityRequester(entity: AgoraEntity, requester: String): Unit = {
    authorizeEntityRequester(getEntityACLs(entity, requester), entity, requester)
  }

  private def authorizeEntityRequester(acls: Seq[AccessControl], entity: AgoraEntity, requester: String): Unit = {
    if (!acls.exists(_.roles.canManage))
      throw AgoraEntityAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  private def checkSameRequester(requester: String, userToModify: String):Unit = {
    if (requester.equalsIgnoreCase(userToModify)) {
      throw PermissionModificationException()
    }
  }

  private def checkSameRequesterAndPermissions(currentACL: Option[AccessControl], newACL: AccessControl): Unit = {
    currentACL match {
      case Some(x) =>
        if (x.user.equalsIgnoreCase(newACL.user) && x.roles != newACL.roles) {
          throw PermissionModificationException()
        }
      case _ => Unit
    }
  }

  private def getNamespaceACLs(entity: AgoraEntity, requester: String): Seq[AccessControl] = {
    NamespacePermissionsClient.listNamespacePermissions(entity).filter {
      perm => perm.user.equals(requester) || perm.user.equals("public")
    }
  }

  private def getEntityACLs(entity: AgoraEntity, requester: String): Seq[AccessControl] = {
    AgoraEntityPermissionsClient.listEntityPermissions(entity).filter {
      perm => perm.user.equals(requester) || perm.user.equals("public")
    }
  }

}
