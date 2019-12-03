package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class NamespacePermissionsClient(profile: JdbcProfile) extends PermissionsClient(profile) {

  def alias(entity: AgoraEntity): String =
    entity.namespaceAlias

  def getNamespacePermission(entity: AgoraEntity, userEmail: String)
                            (implicit executionContext: ExecutionContext): ReadWriteAction[AgoraPermissions] =
    getPermission(entity, userEmail)

  def listNamespacePermissions(entity: AgoraEntity)
                              (implicit executionContext: ExecutionContext): ReadWriteAction[Seq[AccessControl]] =
    listPermissions(entity)

  def insertNamespacePermission(entity: AgoraEntity, userAccess: AccessControl)
                               (implicit executionContext: ExecutionContext): ReadWriteAction[Int] =
    insertPermission(entity, userAccess)

  def editNamespacePermission(entity: AgoraEntity, userAccess: AccessControl)
                             (implicit executionContext: ExecutionContext): ReadWriteAction[Int] =
    editPermission(entity, userAccess)

  def deleteNamespacePermission(entity: AgoraEntity, userEmail: String)
                               (implicit executionContext: ExecutionContext): ReadWriteAction[Int] =
    deletePermission(entity, userEmail)
}
