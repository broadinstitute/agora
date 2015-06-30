package org.broadinstitute.dsde.agora.server.business

import cromwell.parser.WdlParser
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}

class AgoraBusiness {
  def agoraUrl(entity: AgoraEntity): String = {
    hasNamespaceNameId(entity) match {
      case true => AgoraConfig.urlFromType(entity.entityType) + entity.namespace.get + "/" + entity.name.get + "/" + entity.snapshotId.get
      case false => ""
    }
  }

  def hasNamespaceNameId(entity: AgoraEntity): Boolean = {
    entity.namespace.exists(_.trim.nonEmpty) && entity.name.exists(_.trim.nonEmpty) && entity.snapshotId.nonEmpty
  }

  def addUrl(entity: AgoraEntity): AgoraEntity = {
    entity.copy(url = Option(agoraUrl(entity)))
  }

  def insert(agoraEntity: AgoraEntity, username: String): AgoraEntity = {
    val entityWithId = AgoraDao.createAgoraDao(agoraEntity.entityType).insert(agoraEntity)
    entityWithId.copy(url = Option(agoraUrl(entityWithId)))
  }

  def find(agoraSearch: AgoraEntity,
           agoraProjection: Option[AgoraEntityProjection],
           entityTypes: Seq[AgoraEntityType.EntityType],
           username: String): Seq[AgoraEntity] = {
    AgoraDao.createAgoraDao(entityTypes).find(agoraSearch, agoraProjection).map(
      entity => entity.copy(url = Option(agoraUrl(entity)))
    )
  }

  def findSingle(namespace: String,
                 name: String,
                 snapshotId: Int,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): Option[AgoraEntity] = {
    val optEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId)
    optEntity match {
      case Some(foundEntity) => Some(foundEntity.copy(url = Option(agoraUrl(foundEntity))))
      case None => None
    }
  }

  def findSingle(entity: AgoraEntity, entityTypes: Seq[AgoraEntityType.EntityType], username: String) = {
    val optEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(entity)
    optEntity match {
      case Some(foundEntity) => Some(foundEntity.copy(url = Option(agoraUrl(foundEntity))))
      case None => None
    }
  }

  def findSingle(entity: AgoraEntity, username: String): Option[AgoraEntity] = {
    val optEntity = AgoraDao.createAgoraDao(entity.entityType).findSingle(entity)
    optEntity match {
      case Some(foundEntity) => Some(foundEntity.copy(url = Option(agoraUrl(foundEntity))))
      case None => None
    }
  }

  /**
   * Used by cromwell to retrieve the WDL for tasks or workflows that have been pushed into the methods repository
   * @param importString A string encoding the namespace, name and version of the method to look up (looks like: "methods://broad.grep.3")
   * @return The payload of the method, if found, otherwise an empty string.
   */
  // Looks like "methods://broad.grep.3"
  def importResolver(importString: String, username: String): String = {
    val importStringPattern = """^methods\:\/\/(\S+)\.(\S+).(\d+)""".r
    importString match {
      case importStringPattern(namespace, name, snapshotId) =>
        findSingle(namespace, name, snapshotId.toInt, Seq(AgoraEntityType.Task, AgoraEntityType.Workflow), username)
          .getOrElse(AgoraEntity(entityType = Option(AgoraEntityType.Task))).payload.getOrElse("")
      case _ => throw new WdlParser.SyntaxError("Unrecognized import statement format: " + importStringPattern)
    }
  }
}
