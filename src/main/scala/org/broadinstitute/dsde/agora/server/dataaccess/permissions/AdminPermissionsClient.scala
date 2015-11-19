package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

object AdminPermissionsClient extends PermissionsClient {

  def listAdminUsers = listAdmins

  def alias(entity: AgoraEntity) =
    entity.namespace.get + "." + entity.name.get + "." + entity.snapshotId.get
}
