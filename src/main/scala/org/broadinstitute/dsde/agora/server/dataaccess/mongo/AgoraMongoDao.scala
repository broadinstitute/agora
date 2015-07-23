
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports
import com.mongodb.util.JSON
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoDao._
import org.broadinstitute.dsde.agora.server.dataaccess.{AgoraDao, AgoraEntityNotFoundException}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityProjection._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection}
import spray.json._

object AgoraMongoDao {
  val MongoDbIdField = "_id"
  val CounterSequenceField = "seq"
  val KeySeparator = ":"
  val DefaultFindProjection = Option(new AgoraEntityProjection(Seq.empty[String], Seq[String]("payload", "documentation")))

  def EntityToMongoDbObject(entity: AgoraEntity): DBObject = {
    //we need to remove url if it exists since we don't store it in the document
    JSON.parse(entity.copy(url = None).toJson.toString()).asInstanceOf[DBObject]
  }

  def MongoDbObjectToEntity(mongoDBObject: DBObject): AgoraEntity = mongoDBObject.toString.parseJson.convertTo[AgoraEntity]
}

/**
 * The data access object for agora's mongo database. If more than one collection is specified we will query all
 * collections. However, only a single collection is allowed for doing insert operations.
 * @param collections The collections to query. In order to insert a single colleciton must be specified.
 */
class AgoraMongoDao(collections: Seq[MongoCollection]) extends AgoraDao {

  def assureSingleCollection: MongoCollection = {
    if (collections.size != 1) throw new IllegalArgumentException("Multiple collections defined. Only a single collection is supported for this operation")
    else collections.head
  }

  /**
   * On insert we query for the given namespace/name if it exists we increment the snapshotId and store a new one.
   * @param entity The entity to store.
   * @return The entity that was stored.
   */
  override def insert(entity: AgoraEntity): AgoraEntity = {
    val collection = assureSingleCollection
    //update the snapshotId
    val id = getNextId(entity)
    val entityWithId = entity.copy(snapshotId = Option(id))

    //insert the entity
    val dbEntityToInsert = EntityToMongoDbObject(entityWithId)
    collection.insert(dbEntityToInsert)
    findSingle(entityWithId)
  }

  def getNextId(entity: AgoraEntity): Int = {
    //first check to see if we have a sequence
    val counterCollection = getCountersCollection
    val counterId: String = entity.namespace.get + KeySeparator + entity.name.get
    val counterQuery = MongoDbIdField $eq counterId

    //if we don't have a sequence create one
    if (counterCollection.findOne(counterQuery).isEmpty) {
      counterCollection.insert(MongoDBObject(MongoDbIdField -> counterId, CounterSequenceField -> 0))
    }

    //find and modify the sequence
    val currentCount = counterCollection.findAndModify(query = counterQuery, update = $inc(CounterSequenceField -> 1), fields = null, sort = null,
      remove = false, upsert = false, returnNew = true)

    //return new sequence
    currentCount.get(CounterSequenceField).asInstanceOf[Int]
  }

  override def findSingle(entity: AgoraEntity): AgoraEntity = {
    val entityVector = find(EntityToMongoDbObject(entity), None)
    entityVector.length match {
      case 1 => entityVector.head
      case 0 => throw new AgoraEntityNotFoundException(entity)
      case _ => throw new Exception("Found > 1 documents matching: " + entity.toString)
    }
  }

  override def find(entity: AgoraEntity, projectionOpt: Option[AgoraEntityProjection]): Seq[AgoraEntity] = {
    projectionOpt match {
      case Some(projection) => find(EntityToMongoDbObject(entity), projectionOpt)
      case None => find(EntityToMongoDbObject(entity), DefaultFindProjection)
    }
  }

  def find(query: Imports.DBObject, projection: Option[AgoraEntityProjection]) = {
    collections.flatMap {
      collection =>
        collection.find(query, projectionToDBProjections(projection)).map {
          dbObject =>
            MongoDbObjectToEntity(dbObject)
        }
    }
  }

  def projectionToDBProjections(projectionOpt: Option[AgoraEntityProjection]): Imports.DBObject = {
    projectionOpt match {
      case Some(projection) =>
        val builder = MongoDBObject.newBuilder
        projection.excludedFields.foreach(field => builder += field -> 0)

        //we can't currently mix excluded and included fields This is a limitation of mongo.
        if (projection.excludedFields.isEmpty) {
          projection.includedFields.foreach(field => builder += field -> 1)
          RequiredProjectionFields.foreach(field => builder += field -> 1)
        }
        builder.result()
      case None => MongoDBObject()
    }
  }

  override def findSingle(namespace: String, name: String, snapshotId: Int): AgoraEntity = {
    val entity = AgoraEntity(namespace = Option(namespace), name = Option(name), snapshotId = Option(snapshotId))
    findSingle(entity)
  }
}
