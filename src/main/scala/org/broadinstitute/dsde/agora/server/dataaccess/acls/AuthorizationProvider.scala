package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AuthorizationProvider {
  private def filterAuthorizedEntityByReadPermissions(authEntity: AuthorizedAgoraEntity): Option[AgoraEntity] = {
    if(authEntity.authorization.canRead) authEntity.entity else None
  }

  def authorizationsForEntity(agoraEntity: Option[AgoraEntity], username: String): AuthorizedAgoraEntity

  def authorizationsForEntities(agoraEntities: Seq[AgoraEntity], username: String): Seq[AuthorizedAgoraEntity]

  def filterByReadPermissions(entities: Seq[AgoraEntity], username: String): Seq[AgoraEntity] = {
    authorizationsForEntities(entities, username).flatMap(authEntity => filterAuthorizedEntityByReadPermissions(authEntity))
  }

  def filterByReadPermissions(agoraEntity: Option[AgoraEntity], username: String): Option[AgoraEntity] = {
    filterAuthorizedEntityByReadPermissions(authorizationsForEntity(agoraEntity, username))
  }
}


