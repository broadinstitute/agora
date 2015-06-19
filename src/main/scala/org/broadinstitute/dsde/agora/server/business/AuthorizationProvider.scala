package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AuthorizationProvider {
  def authorizationsForEntity(agoraEntity: AgoraEntity, username: String): AuthorizedAgoraEntity

  def authorizationsForEntities(agoraEntities: Seq[AgoraEntity], username: String): Seq[AuthorizedAgoraEntity]
}


