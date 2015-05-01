
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

  def findByName(name: String): Seq[AgoraEntity]

  def findPayloadByRegex(regex: String): Seq[AgoraEntity]

  def find(namespace: String, name: String, id: Int): AgoraEntity
}
