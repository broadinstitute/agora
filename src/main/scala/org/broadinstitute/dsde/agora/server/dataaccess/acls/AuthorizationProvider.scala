package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AuthorizationProvider {

  /**
   * Create permissions for the given entity
   * @param agoraEntity The entity requiring permissions
   */
  def createEntityAuthorizations(agoraEntity: AgoraEntity, username: String): Unit

  /**
   * Provides the AgoraPermissions for a given user on a given AgoraEntity
   * @param agoraEntity The entity to check user permissions on
   * @param username The name of the user whose authorizations you wish to check
   * @return AgoraPermissions
   */
  def namespaceAuthorization(agoraEntity: AgoraEntity, username: String): AgoraPermissions

  def entityAuthorization(agoraEntity: AgoraEntity, username: String): AgoraPermissions

  /**
   * Given a sequence of AgoraEntities, return a new sequence containing only those a given user is authorized to read
   * @param entities The sequence of entities to filter
   * @param username The user whose permissions you wish to filter by
   * @return Seq[AgoraEntity], filtered by user permissions
   */
  // Currently not parallelized! May need to be overridden for optimization.
  def filterByReadPermissions(entities: Seq[AgoraEntity], username: String): Seq[AgoraEntity] =
    entities
      .filter(entity => isAuthorizedForRead(entity, username))

  /**
   * Check whether a user has permission to create the given entity within Agora
   * @param agoraEntity The entity to check user permissions on
   * @param username The name of the user whose authorizations you wish to check
   * @return
   */
  def isAuthorizedForCreation(agoraEntity: AgoraEntity, username: String): Boolean = {
    namespaceAuthorization(agoraEntity, username).canCreate
  }

  /**
   * Check whether a user has permission to read the given entity from Agora
   * @param agoraEntity The entity to check user permissions on
   * @param username The name of the user whose authorizations you wish to check
   * @return
   */
  def isAuthorizedForRead(agoraEntity: AgoraEntity, username: String): Boolean = {
    entityAuthorization(agoraEntity, username).canRead
  }
}
