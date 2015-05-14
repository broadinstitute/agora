package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

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
    val entityWithId = AgoraDao.createAgoraDao.insert(agoraEntity)
    entityWithId.copy(url = Option(agoraUrl(entityWithId)))
  }

  def find(agoraSearch: AgoraEntity): Seq[AgoraEntity] = {
    AgoraDao.createAgoraDao.find(agoraSearch).map(entity => entity.copy(url = Option(agoraUrl(entity))))
  }

  def findSingle(namespace: String, name: String, snapshotId: Int): Option[AgoraEntity] = {
    AgoraDao.createAgoraDao.findSingle(namespace, name, snapshotId).map(entity => entity.copy(url = Option(agoraUrl(entity))))
  }

  def findSingle(entity: AgoraEntity): Option[AgoraEntity] = {
    AgoraDao.createAgoraDao.findSingle(entity).map(entity => entity.copy(url = Option(agoraUrl(entity))))
  }

}
