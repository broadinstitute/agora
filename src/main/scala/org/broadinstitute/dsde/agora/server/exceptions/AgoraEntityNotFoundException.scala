package org.broadinstitute.dsde.agora.server.exceptions

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class AgoraEntityNotFoundException(agoraEntity: AgoraEntity) extends Exception {
  override def getMessage: String = {
    val namespaceStr = agoraEntity.namespace.getOrElse("[namespace unknown]")
    val nameStr = agoraEntity.name.getOrElse("[name unknown]")
    val snapshotStr = agoraEntity.snapshotId.map("/"+_).getOrElse("")

    s"Methods Repository entity $namespaceStr/$nameStr$snapshotStr not found."
  }
}
