package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AuthorizationProvider {

  /**
   * Create permissions for the given entity
   */
  def createEntityAuthorizations(agoraEntity: AgoraEntity, username: String): Unit

  /**
   * Provides the AgoraPermissions for a given user on a given AgoraEntity
   *
   * First checks if the entity exists. If it doesn't then it may be created.
   * Otherwise, returns permissions on the existing entity.
   */
  def namespaceAuthorization(agoraEntity: AgoraEntity, username: String): AgoraPermissions
  def entityAuthorization(agoraEntity: AgoraEntity, username: String): AgoraPermissions

  /**
   * Given a sequence of AgoraEntities, return a new sequence containing only those a given user is authorized to read
   */
  // Currently not parallelized! May need to be overridden for optimization.
  def filterByReadPermissions(entities: Seq[AgoraEntity], username: String): Seq[AgoraEntity] =
    entities
      .filter(entity => isAuthorizedForRead(entity, username))

  /**
   * Check whether a user has permission to create the given entity within Agora
   */
  def isAuthorizedForCreation(agoraEntity: AgoraEntity, username: String): Boolean = {
    namespaceAuthorization(agoraEntity, username).canCreate
  }

  /**
   * Check whether a user has permission to read the given entity from Agora
   */
  def isAuthorizedForRead(agoraEntity: AgoraEntity, username: String): Boolean = {
    entityAuthorization(agoraEntity, username).canRead
  }
}
