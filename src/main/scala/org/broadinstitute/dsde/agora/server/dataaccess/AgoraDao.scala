
package org.broadinstitute.dsde.agora.server.dataaccess

import com.mongodb.casbah.MongoCollection
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoDao
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.bson.types.ObjectId

object AgoraDao {
  //factory method (currently only supports mongo)
  def createAgoraDao(entityType: Option[AgoraEntityType.EntityType]): AgoraDao = {
    new AgoraMongoDao(getCollectionsByEntityType(entityType))
  }

  def createAgoraDao(collection: MongoCollection): AgoraDao = {
    new AgoraMongoDao(Seq(collection))
  }

  def createAgoraDao(entityTypes: Seq[AgoraEntityType.EntityType]): AgoraDao ={
    new AgoraMongoDao(entityTypes.flatMap(entityType => getCollectionsByEntityType(Option(entityType))))
  }
}

trait AgoraDao {
  def insert(entity: AgoraEntity): AgoraEntity

  def findByAliases(aliases: List[String], entity: AgoraEntity, projectionOpt: Option[AgoraEntityProjection]): Seq[AgoraEntity]

  def find(entity: AgoraEntity, agoraProjection: Option[AgoraEntityProjection] = None): Seq[AgoraEntity]

  def findSingle(entity: AgoraEntity): AgoraEntity

  def findSingle(namespace: String, name: String, snapshotId: Int): AgoraEntity

  def findConfigurations(id: ObjectId): Seq[AgoraEntity]

  def findConfigurations(ids: Seq[ObjectId]): Seq[AgoraEntity]
}
