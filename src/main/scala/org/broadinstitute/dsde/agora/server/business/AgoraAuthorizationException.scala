
package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class AgoraAuthorizationException(agoraPermissions: AgoraPermissions, agoraEntity: AgoraEntity) extends Exception {
  override def getMessage: String = {
    s"Authorization exception attempting to exercise permission ${agoraPermissions.permissions} " +
      s"on entity ${agoraEntity.namespace.get}.${agoraEntity.name.get}.${agoraEntity.snapshotId.get}."
  }
}
