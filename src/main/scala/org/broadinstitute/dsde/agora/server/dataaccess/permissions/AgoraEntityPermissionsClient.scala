package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

object AgoraEntityPermissionsClient extends PermissionsClient {

  def alias(entity: AgoraEntity) =
    entity.namespace.get + "." + entity.name.get + "." + entity.snapshotId.get

  def getEntityPermission(entity: AgoraEntity, userEmail: String) =
    getPermission(entity, userEmail)

  def listEntityPermissions(entity: AgoraEntity) =
    listPermissions(entity)

  def listEntityOwners(entity: AgoraEntity) =
    listOwners(entity)

  def insertEntityPermission(entity: AgoraEntity, userAccess: AccessControl) =
    insertPermission(entity, userAccess)

  def editEntityPermission(entity: AgoraEntity, userAccess: AccessControl) =
    editPermission(entity,userAccess)

  def deleteEntityPermission(entity: AgoraEntity, userEmail: String) =
    deletePermission(entity, userEmail)

}
