package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.jdbc.JdbcProfile

class AgoraEntityPermissionsClient(profile: JdbcProfile) extends PermissionsClient(profile) {

  def getEntityPermission(entity: AgoraEntity, userEmail: String) =
    getPermission(entity, userEmail)

  def listEntityPermissions(entity: AgoraEntity) =
    listPermissions(entity)

  def insertEntityPermission(entity: AgoraEntity, userAccess: AccessControl) =
    insertPermission(entity, userAccess)

  def editEntityPermission(entity: AgoraEntity, userAccess: AccessControl) =
    editPermission(entity,userAccess)

  def deleteEntityPermission(entity: AgoraEntity, userEmail: String) =
    deletePermission(entity, userEmail)

}
