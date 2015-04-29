
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.Imports._
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
}

class AgoraMongoDao(collection: MongoCollection) extends AgoraDao {

  override def findPayloadByRegex(regex: String): Seq[AgoraEntity] = {
    find("payload" $regex regex)
  }

  override def findByName(name: String): Seq[AgoraEntity] = {
    find("metadata.name" $eq name)
  }

  /**
   * On insert we query for the given namespace/name if it exists we increment the id and store a new one.
   * @param entity The entity to store.
   * @return The entity that was stored.
   */
  override def insert(entity: AgoraEntity): AgoraEntity = {
    //update the id
    val id = getNextId(entity)
    entity.metadata.id = Option(id)

    //insert the entity
    val entityToInsert = grater[AgoraEntity].asDBObject(entity)
    collection.insert(entityToInsert)
    find(entityToInsert).head
  }

  def getNextId(entity: AgoraEntity): Int = {
    //first check to see if we have a sequence
    val counterCollection = getCollection(collection.getDB, CounterCollectionName)
    val counterId: String = entity.metadata.namespace + KeySeparator + entity.metadata.name
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
    (for (entity <- collection.find(query)) yield grater[AgoraEntity].asObject(entity)).toVector
  }

  override def find(namespace: String, name: String, id: Int): AgoraEntity = {
    find($and("metadata.namespace" $eq namespace, "metadata.name" $eq name, "metadata.id" $eq id)).head
  }
}
