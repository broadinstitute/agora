package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

/**
 * Created by dshiga on 5/13/15.
 */
object AgoraBusiness {

  def agoraUrl(entity: AgoraEntity): String = {
    AgoraConfig.methodsUrl + entity.namespace.get + "/" + entity.name.get + "/" + entity.snapshotId.get
  }

  def addUrl(entity: AgoraEntity): AgoraEntity = {
    entity.copy(url = Option(agoraUrl(entity)))
  }

  def insert(agoraEntity: AgoraEntity): AgoraEntity = {
    AgoraDao.createAgoraDao.insert(agoraEntity).copy(url = Option(agoraUrl(agoraEntity)))
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
