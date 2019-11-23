package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class AgoraEntityPermissionsClient(profile: JdbcProfile) extends PermissionsClient(profile) {

  def alias(entity: AgoraEntity) =
    entity.entityAlias

  def getEntityPermission(entity: AgoraEntity, userEmail: String)
                         (implicit executionContext: ExecutionContext): ReadWriteAction[AgoraPermissions] =
    getPermission(entity, userEmail)

  def listEntityPermissions(entity: AgoraEntity)
                           (implicit executionContext: ExecutionContext): ReadWriteAction[Seq[AccessControl]] =
    listPermissions(entity)

  def insertEntityPermission(entity: AgoraEntity, userAccess: AccessControl)
                            (implicit executionContext: ExecutionContext): ReadWriteAction[Int] =
    insertPermission(entity, userAccess)

  def editEntityPermission(entity: AgoraEntity, userAccess: AccessControl)
                          (implicit executionContext: ExecutionContext): ReadWriteAction[Int] =
    editPermission(entity,userAccess)

  def deleteEntityPermission(entity: AgoraEntity, userEmail: String)
                            (implicit executionContext: ExecutionContext): ReadWriteAction[Int] =
    deletePermission(entity, userEmail)

}
