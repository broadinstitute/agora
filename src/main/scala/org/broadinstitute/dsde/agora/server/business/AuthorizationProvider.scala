package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AuthorizationProvider {
  def authorizationsForEntity(agoraEntity: Option[AgoraEntity], username: String): AuthorizedAgoraEntity

  def authorizationsForEntities(agoraEntities: Seq[AgoraEntity], username: String): Seq[AuthorizedAgoraEntity]

  def filterByReadPermissions(authEntities: Seq[AuthorizedAgoraEntity]): Seq[AgoraEntity] = {
    authEntities.flatMap(authEntity => filterByReadPermissions(authEntity))
  }

  def filterByReadPermissions(authEntity: AuthorizedAgoraEntity): Option[AgoraEntity] = {
    if (authEntity.authorization.canRead) authEntity.entity else None
  }
}


