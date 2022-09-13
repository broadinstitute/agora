
package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoDao
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.bson.types.ObjectId
import org.mongodb.scala._

import scala.concurrent.{ExecutionContext, Future}

object AgoraDao {
  //factory method (currently only supports mongo)
  def createAgoraDao(entityType: Option[AgoraEntityType.EntityType]): AgoraDao = {
    new AgoraMongoDao(getCollectionsByEntityType(entityType))
  }

  def createAgoraDao(collection: MongoCollection[Document]): AgoraDao = {
    new AgoraMongoDao(Seq(collection))
  }

  def createAgoraDao(entityTypes: Seq[AgoraEntityType.EntityType]): AgoraDao ={
    new AgoraMongoDao(entityTypes.flatMap(entityType => getCollectionsByEntityType(Option(entityType))))
  }
}

trait AgoraDao {
  def insert(entity: AgoraEntity)(implicit executionContext: ExecutionContext): Future[AgoraEntity]

  def find(entity: AgoraEntity, agoraProjection: Option[AgoraEntityProjection] = None)
          (implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]]

  def findSingle(entity: AgoraEntity)(implicit executionContext: ExecutionContext): Future[AgoraEntity]

  def findSingle(namespace: String, name: String, snapshotId: Int)
                (implicit executionContext: ExecutionContext): Future[AgoraEntity]

  def findConfigurations(id: ObjectId)(implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]]

  def findConfigurations(ids: Seq[ObjectId])(implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]]
}
