package org.broadinstitute.dsde.agora.server.exceptions

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class NamespaceAuthorizationException(agoraPermissions: AgoraPermissions, agoraEntity: AgoraEntity, user: String) extends Exception {
  override def getMessage: String = {
    s"Authorization exception for user ${user} attempting to exercise permission ${agoraPermissions.toString} " +
      s"on namespace ${agoraEntity.namespace.get}"
  }
}
