package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

class NamespacePermissionsClient extends PermissionsClient {

  def alias(entity: AgoraEntity) =
    entity.namespace.get

  def getNamespacePermission(entity: AgoraEntity, userEmail: String) =
    getPermission(entity, userEmail)

  def listNamespacePermissions(entity: AgoraEntity) =
    listPermissions(entity)

  def insertNamespacePermission(entity: AgoraEntity, userAccess: AccessControl) =
    insertPermission(entity, userAccess)

  def editNamespacePermission(entity: AgoraEntity, userAccess: AccessControl) =
    editPermission(entity, userAccess)

  def deleteNamespacePermission(entity: AgoraEntity, userEmail: String) =
    deletePermission(entity, userEmail)

}
