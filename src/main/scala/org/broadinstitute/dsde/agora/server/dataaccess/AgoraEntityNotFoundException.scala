
package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class AgoraEntityNotFoundException(agoraEntity: AgoraEntity) extends Exception {
  override def getMessage: String = {
    s"Entity ${agoraEntity.namespace.getOrElse("No Namespace")}" +
      s".${agoraEntity.name.getOrElse("No Name")}" +
      s".${agoraEntity.snapshotId.getOrElse(-1)} not found."
  }
}