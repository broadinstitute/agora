package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

class AdminPermissionsClient(profile: JdbcProfile) extends PermissionsClient(profile) {

  def listAdminUsers()(implicit executionContext: ExecutionContext): ReadWriteAction[Seq[String]] = listAdmins()

  def alias(entity: AgoraEntity): String =
    entity.entityAlias
}
