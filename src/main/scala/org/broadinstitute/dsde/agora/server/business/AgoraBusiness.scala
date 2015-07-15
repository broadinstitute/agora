package org.broadinstitute.dsde.agora.server.business

import cromwell.binding._
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraPermissions, AuthorizationProvider}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.joda.time.DateTime
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions._

class AgoraBusiness(authorizationProvider: AuthorizationProvider) {

  def insert(agoraEntity: AgoraEntity, username: String): AgoraEntity = {
    if (authorizationProvider.isAuthorizedForCreation(agoraEntity, username)) {
      validatePayload(agoraEntity, username)
      val entityWithId = AgoraDao.createAgoraDao(agoraEntity.entityType).insert(agoraEntity.copy(createDate = Option(new DateTime())))
      authorizationProvider.createEntityAuthorizations(entityWithId, username)
      entityWithId.addUrl()
    } else {
      throw new AgoraAuthorizationException(AgoraPermissions(Create), agoraEntity)
    }
  }

  def find(agoraSearch: AgoraEntity,
           agoraProjection: Option[AgoraEntityProjection],
           entityTypes: Seq[AgoraEntityType.EntityType],
           username: String): Seq[AgoraEntity] = {
    val entities = AgoraDao.createAgoraDao(entityTypes).find(agoraSearch, agoraProjection).map(
      entity => entity.addUrl()
    )
    authorizationProvider.filterByReadPermissions(entities, username)
  }

  def findSingle(namespace: String,
                 name: String,
                 snapshotId: Int,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): Option[AgoraEntity] = {
    val optEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId)
    optEntity match {
      case Some(foundEntity) =>
        if (authorizationProvider.isAuthorizedForRead(foundEntity, username)) {
          Some(foundEntity.addUrl())
        }
        else throw new AgoraAuthorizationException(AgoraPermissions(Read), foundEntity)
      case None => None
    }
  }

  def findSingle(entity: AgoraEntity, entityTypes: Seq[AgoraEntityType.EntityType], username: String): Option[AgoraEntity] = {
    findSingle(entity.namespace.get, entity.name.get, entity.snapshotId.get, entityTypes, username)
  }

  def findSingle(entity: AgoraEntity, username: String): Option[AgoraEntity] = {
    val optEntity = AgoraDao.createAgoraDao(entity.entityType).findSingle(entity)
    optEntity match {
      case Some(foundEntity) =>
        if (authorizationProvider.isAuthorizedForRead(foundEntity, username)) {
          Some(foundEntity.addUrl())
        }
        else throw new AgoraAuthorizationException(AgoraPermissions(Read), foundEntity)
      case None => None
    }
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