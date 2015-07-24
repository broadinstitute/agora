package org.broadinstitute.dsde.agora.server.business

import cromwell.binding._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraPermissions, AuthorizationProvider}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}

class AgoraBusiness(authorizationProvider: AuthorizationProvider) {

  def insert(agoraEntity: AgoraEntity, username: String): AgoraEntity = {
    if (authorizationProvider.isAuthorizedForCreation(agoraEntity, username)) {
      validatePayload(agoraEntity, username)

      val entityWithId = AgoraDao.createAgoraDao(agoraEntity.entityType).insert(agoraEntity.addDate())
      authorizationProvider.createEntityAuthorizations(entityWithId, username)
      entityWithId.addUrl()

    } else {
      throw new EntityAuthorizationException(AgoraPermissions(Create), agoraEntity)
    }
  }

  def find(agoraSearch: AgoraEntity,
           agoraProjection: Option[AgoraEntityProjection],
           entityTypes: Seq[AgoraEntityType.EntityType],
           username: String): Seq[AgoraEntity] = {
    val entities = AgoraDao.createAgoraDao(entityTypes)
      .find(agoraSearch, agoraProjection)
      .map(entity => entity.addUrl())

    authorizationProvider.filterByReadPermissions(entities, username)
  }

  def findSingle(namespace: String,
                 name: String,
                 snapshotId: Int,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): AgoraEntity = {
    val foundEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId)

    if (authorizationProvider.isAuthorizedForRead(foundEntity, username))
      foundEntity.addUrl()
    else
      throw new EntityAuthorizationException(AgoraPermissions(Read), foundEntity)
  }

  def findSingle(entity: AgoraEntity, entityTypes: Seq[AgoraEntityType.EntityType], username: String): AgoraEntity = {
    findSingle(entity.namespace.get, entity.name.get, entity.snapshotId.get, entityTypes, username)
  }

  private def validatePayload(agoraEntity: AgoraEntity, username: String): Unit = {
    agoraEntity.entityType.get match {
      case AgoraEntityType.Task =>
        WdlNamespace.load(agoraEntity.payload.get)

      case AgoraEntityType.Workflow =>
        val resolver = MethodImportResolver(username, this, authorizationProvider)
        WdlNamespace.load(agoraEntity.payload.get, resolver.importResolver _)

      case AgoraEntityType.Configuration =>
      //add config validation here
    }
  }
}