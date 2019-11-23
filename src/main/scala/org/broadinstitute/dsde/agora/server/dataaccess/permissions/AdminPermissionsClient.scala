package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.jdbc.JdbcProfile

class AdminPermissionsClient(profile: JdbcProfile) extends PermissionsClient(profile) {

  def listAdminUsers = listAdmins

  def alias(entity: AgoraEntity): String =
    entity.entityAlias
}
