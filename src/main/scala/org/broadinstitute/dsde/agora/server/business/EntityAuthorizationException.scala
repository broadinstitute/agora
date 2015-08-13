
package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class NamespaceAuthorizationException(agoraPermissions: AgoraPermissions, agoraEntity: AgoraEntity, user: String) extends Exception {
  override def getMessage: String = {
    s"Authorization exception for user ${user} attempting to exercise permission ${agoraPermissions.toString} " +
      s"on namespace ${agoraEntity.namespace.get}"
  }
}

case class EntityAuthorizationException(agoraPermissions: AgoraPermissions, agoraEntity: AgoraEntity, user: String) extends Exception {
  override def getMessage: String = {
    s"Authorization exception for user ${user} attempting to exercise permission ${agoraPermissions.toString} " +
      s"on entity ${agoraEntity.namespace.getOrElse("No Namespace")}" +
      s".${agoraEntity.name.getOrElse("No Name")}" +
      s".${agoraEntity.snapshotId.getOrElse(-1)}."
  }
}
