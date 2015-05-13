
package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoDao
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

object AgoraDao {
  //factory method (currently only supports mongo)
  def createAgoraDao: AgoraDao = {
    new AgoraMongoDao(getMethodsCollection(getMongoClient))
  }
}

trait AgoraDao {
  def insert(entity: AgoraEntity): AgoraEntity

  def find(entity: AgoraEntity): Seq[AgoraEntity]

  def findSingle(entity: AgoraEntity): Option[AgoraEntity]

  def findSingle(namespace: String, name: String, snapshotId: Int): Option[AgoraEntity]
}
