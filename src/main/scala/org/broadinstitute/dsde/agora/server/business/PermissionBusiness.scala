package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraEntityPermissionsClient, AgoraPermissions, NamespacePermissionsClient}
import AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityAuthorizationException, AgoraException, NamespaceAuthorizationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import spray.http.StatusCodes.Conflict


class PermissionBusiness {

  def listNamespacePermissions(entity: AgoraEntity, requester: String): Seq[AccessControl] = {
    authorizeNamespaceRequester(getNamespaceACLs(entity, requester), entity, requester)
    NamespacePermissionsClient.listNamespacePermissions(entity)
  }

  def insertNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    // Inserts can result in an edit in the DAO layer
    val namespaceACLs = getNamespaceACLs(entity, requester)
    checkSameRequesterAndPermissions(namespaceACLs.find(acl => acl.user.equals(requester)), accessObject)
    authorizeNamespaceRequester(namespaceACLs, entity, requester)
    NamespacePermissionsClient.insertNamespacePermission(entity, accessObject)
  }

  def batchNamespacePermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Int = {
    accessObjectList.map(accessObject => insertNamespacePermission(entity, requester, accessObject)).sum
  }

  def editNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    val namespaceACLs = getNamespaceACLs(entity, requester)
    checkSameRequesterAndPermissions(namespaceACLs.find(acl => acl.user.equals(requester)), accessObject)
    authorizeNamespaceRequester(namespaceACLs, entity, requester)
    NamespacePermissionsClient.editNamespacePermission(entity, accessObject)
  }

  def deleteNamespacePermission(entity: AgoraEntity, requester: String, userToRemove: String): Int = {
    checkSameRequester(requester, userToRemove)
    authorizeNamespaceRequester(getNamespaceACLs(entity, requester), entity, requester)
    NamespacePermissionsClient.deleteNamespacePermission(entity, userToRemove)
  }

  def listEntityPermissions(entity: AgoraEntity, requester: String): Seq[AccessControl] = {
    authorizeEntityRequester(getEntityACLs(entity, requester), entity, requester)
    AgoraEntityPermissionsClient.listEntityPermissions(entity)
  }

  def insertEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    // Inserts can result in an edit in the DAO layer
    val entityACLs = getEntityACLs(entity, requester)
    checkSameRequesterAndPermissions(entityACLs.find(acl => acl.user.equals(requester)), accessObject)
    authorizeEntityRequester(entityACLs, entity, requester)
    AgoraEntityPermissionsClient.insertEntityPermission(entity, accessObject)
  }

  def batchEntityPermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Int = {
    accessObjectList.map(accessObject => insertEntityPermission(entity, requester, accessObject)).sum
  }

  def editEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    val entityACLs = getEntityACLs(entity, requester)
    checkSameRequesterAndPermissions(entityACLs.find(acl => acl.user.equals(requester)), accessObject)
    authorizeEntityRequester(entityACLs, entity, requester)
    AgoraEntityPermissionsClient.editEntityPermission(entity, accessObject)

  }

  def deleteEntityPermission(entity: AgoraEntity, requester: String, userToRemove: String): Int = {
    checkSameRequester(requester, userToRemove)
    authorizeEntityRequester(getEntityACLs(entity, requester), entity, requester)
    AgoraEntityPermissionsClient.deleteEntityPermission(entity, userToRemove)
  }


  private def authorizeNamespaceRequester(acls: Seq[AccessControl], entity: AgoraEntity, requester: String): Unit = {
    if (!acls.exists(_.roles.canManage))
      throw NamespaceAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  private def authorizeEntityRequester(acls: Seq[AccessControl], entity: AgoraEntity, requester: String): Unit = {
    if (!acls.exists(_.roles.canManage))
      throw AgoraEntityAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  private def checkSameRequester(requester: String, userToModify: String):Unit = {
    if (requester.equalsIgnoreCase(userToModify)) {
      val m = "Unable to remove access control for current user"
      throw AgoraException(m, new Exception(m), Conflict)
    }
  }

  private def checkSameRequesterAndPermissions(currentACL: Option[AccessControl], newACL: AccessControl): Unit = {
    currentACL match {
      case Some(x) =>
        if (x.user.equalsIgnoreCase(newACL.user) && x.roles != newACL.roles) {
          val m = "Unable to modify access control for current user"
          throw AgoraException(m, new Exception(m), Conflict)
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