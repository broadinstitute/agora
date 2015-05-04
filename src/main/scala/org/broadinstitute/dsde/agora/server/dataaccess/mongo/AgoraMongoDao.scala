
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.Imports._

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports
import com.novus.salat._
import com.novus.salat.global._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoDao._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

object AgoraMongoDao {
  val MongoDbIdField = "_id"
  val CounterCollectionName = "counters"
  val CounterSequenceField = "seq"
  val KeySeparator = ":"

  def EntityToMongoDbObject(entity: AgoraEntity): DBObject = grater[AgoraEntity].asDBObject(entity)
  def MongoDbObjectToEntity(mongoDBObject: DBObject): AgoraEntity = grater[AgoraEntity].asObject(mongoDBObject)
}

class AgoraMongoDao(collection: MongoCollection) extends AgoraDao {

  /**
   * On insert we query for the given namespace/name if it exists we increment the id and store a new one.
   * @param entity The entity to store.
   * @return The entity that was stored.
   */
  override def insert(entity: AgoraEntity): AgoraEntity = {
    //update the id
    val id = getNextId(entity)
    entity.id = Option(id)

    //insert the entity
    val dbEntityToInsert = EntityToMongoDbObject(entity)
    collection.insert(dbEntityToInsert)
    findSingle(entity) match {
      case None => throw new Exception("failed to find inserted entity?")
      case foundEntity => foundEntity.get
    }
  }

  def getNextId(entity: AgoraEntity): Int = {
    //first check to see if we have a sequence
    val counterCollection = getCollection(collection.getDB, CounterCollectionName)
    val counterId: String = entity.namespace + KeySeparator + entity.name
    val counterQuery = MongoDbIdField $eq counterId

    //if we don't have a sequence create one
    if (counterCollection.findOne(counterQuery) == None) {
      counterCollection.insert(MongoDBObject(MongoDbIdField -> counterId, CounterSequenceField -> 0))
    }

    //find and modify the sequence
    val currentCount = counterCollection.findAndModify(query = counterQuery, update = $inc(CounterSequenceField -> 1), fields = null, sort = null,
      remove = false, upsert = false, returnNew = true)

    //return new sequence
    currentCount.get(CounterSequenceField).asInstanceOf[Int]
  }

  def find(query: Imports.DBObject) = {
    (for (dbObject <- collection.find(query)) yield MongoDbObjectToEntity(dbObject)).toVector
  }

  override def find(entity: AgoraEntity): Seq[AgoraEntity] = find(EntityToMongoDbObject(entity))

  override def findSingle(entity: AgoraEntity): Option[AgoraEntity] = {
    val entityVector = find(EntityToMongoDbObject(entity))
    entityVector.length match {
      case 1 => Some(entityVector.head)
      case 0 => None
      case _ => throw new Exception("Found > 1 documents matching: " + entity.toString)
    }
  }

  override def findSingle(namespace: String, name: String, id: Int): Option[AgoraEntity] = {
    val entity = AgoraEntity(namespace = Option(namespace), name = Option(name), id = Option(id))
    findSingle(entity)
  }
}
