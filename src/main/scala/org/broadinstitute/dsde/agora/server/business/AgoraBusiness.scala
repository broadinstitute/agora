package org.broadinstitute.dsde.agora.server.business

import cromwell.parser.WdlParser
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraEntity, AgoraEntityProjection}

object AgoraBusiness {

  def agoraUrl(entity: AgoraEntity): String = {
    hasNamespaceNameId(entity) match {
      case true => AgoraConfig.methodsUrl + entity.namespace.get + "/" + entity.name.get + "/" + entity.snapshotId.get
      case false => ""
    }
  }

  def hasNamespaceNameId(entity: AgoraEntity): Boolean = {
    entity.namespace.exists(_.trim.nonEmpty) && entity.name.exists(_.trim.nonEmpty) && entity.snapshotId.nonEmpty
  }

  def addUrl(entity: AgoraEntity): AgoraEntity = {
    entity.copy(url = Option(agoraUrl(entity)))
  }

  def insert(agoraEntity: AgoraEntity): AgoraEntity = {
    val entityWithId = AgoraDao.createAgoraDao(agoraEntity.entityType).insert(agoraEntity)
    entityWithId.copy(url = Option(agoraUrl(entityWithId)))
  }

  def find(agoraSearch: AgoraEntity, agoraProjection: Option[AgoraEntityProjection]): Seq[AgoraEntity] = {
    AgoraDao.createAgoraDao(agoraSearch.entityType).find(agoraSearch, agoraProjection).map(entity => entity.copy(url = Option(agoraUrl(entity))))
  }

  def findSingle(namespace: String, name: String, snapshotId: Int, entityTypes: Seq[AgoraEntityType.EntityType]): Option[AgoraEntity] = {
    AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId).map(entity => entity.copy(url = Option(agoraUrl(entity))))
  }

  def findSingle(entity: AgoraEntity): Option[AgoraEntity] = {
    AgoraDao.createAgoraDao(entity.entityType).findSingle(entity).map(entity => entity.copy(url = Option(agoraUrl(entity))))
  }

  /**
   * Used by cromwell to retrieve the WDL for tasks or workflows that have been pushed into the methods repository
   * @param importString A string encoding the namespace, name and version of the method to look up (looks like: "methods://broad.grep.3")
   * @return The payload of the method, if found, otherwise an empty string.
   */
  // Looks like "methods://broad.grep.3"
  def importResolver(importString: String) : String = {
    val importStringPattern = """^methods\:\/\/(\S+)\.(\S+).(\d+)""".r
    importString match {
      case importStringPattern(namespace, name, snapshotId) =>
        AgoraBusiness.findSingle(namespace, name, snapshotId.toInt, Seq(AgoraEntityType.Task, AgoraEntityType.Workflow))
          .getOrElse(new AgoraEntity(entityType = Option(AgoraEntityType.Task))).payload.getOrElse("")
      case _ => throw new WdlParser.SyntaxError("Unrecognized import statement format: " + importStringPattern)
    }
  }
}
