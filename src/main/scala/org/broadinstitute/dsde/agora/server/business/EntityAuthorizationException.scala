
package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class NamespaceAuthorizationException(agoraPermissions: AgoraPermissions, agoraEntity: AgoraEntity) extends Exception {
  override def getMessage: String = {
    s"Authorization exception attempting to exercise permission ${agoraPermissions.permissions} " +
      s"on namespace ${agoraEntity.namespace.get}"
  }
}

case class EntityAuthorizationException(agoraPermissions: AgoraPermissions, agoraEntity: AgoraEntity) extends Exception {
  override def getMessage: String = {
    s"Authorization exception attempting to exercise permission ${agoraPermissions.permissions} " +
      s"on entity ${agoraEntity.namespace.getOrElse("No Namespace")}" +
      s".${agoraEntity.name.getOrElse("No Name")}" +
      s".${agoraEntity.snapshotId.getOrElse(-1)}."
  }
}
