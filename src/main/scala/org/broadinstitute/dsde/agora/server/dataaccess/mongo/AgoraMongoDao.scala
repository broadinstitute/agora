
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.query.Imports
import com.mongodb.util.JSON
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoDao._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.exceptions.AgoraEntityNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityProjection._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.bson.types.ObjectId
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

  def MongoDbObjectToEntity(mongoDBObject: DBObject): AgoraEntity = {
    mongoDBObject.toString.parseJson.convertTo[AgoraEntity]
  }
}

/**
 * The data access object for agora's mongo database. If more than one collection is specified we will query all
 * collections. However, only a single collection is allowed for doing insert operations.
 * @param collections The collections to query. In order to insert a single colleciton must be specified.
 */
class AgoraMongoDao(collections: Seq[MongoCollection]) extends AgoraDao with LazyLogging {

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
    val insertedEntity = findSingle(MongoDbObjectToEntity(dbEntityToInsert))
    insertedEntity
  }

  override def findSingle(namespace: String, name: String, snapshotId: Int): AgoraEntity = {
    val entity = AgoraEntity(namespace = Option(namespace), name = Option(name), snapshotId = Option(snapshotId))
    findSingle(entity)
  }

  override def findSingle(entity: AgoraEntity): AgoraEntity = {
    val dbEntity = EntityToMongoDbObject(entity)
    val entityVector = find(dbEntity, None)
    entityVector.length match {
      case 1 =>
        val foundEntity = entityVector.head
        addMethodRef(Seq(foundEntity), None).head
      case 0 => throw new AgoraEntityNotFoundException(entity)
      case _ => throw new Exception("Found > 1 documents matching: " + entity.toString)
    }
  }

  override def find(entity: AgoraEntity, projectionOpt: Option[AgoraEntityProjection]): Seq[AgoraEntity] = {
    val projection = projectionOpt match {
      case Some(_) => projectionOpt
      case None => DefaultFindProjection
    }
    val dbEntity = EntityToMongoDbObject(entity)
    val entities = find(dbEntity, projection)
    addMethodRef(entities, projection)
  }

  def findById(ids: Seq[ObjectId], entityCollections: Seq[MongoCollection], projection: Option[AgoraEntityProjection]): Seq[AgoraEntity] = {
    val dbQuery = (MongoDbIdField $in ids)
    val entities = entityCollections.flatMap {
      collection =>
        collection.find(dbQuery, projectionToDBProjections(projection)).map {
          dbObject => MongoDbObjectToEntity(dbObject)
        }
    }
    entities
  }

  // Find all configurations that have specified method id.
  override def findConfigurations(id: ObjectId) = {
    val dbQuery = "methodId" $eq id
    queryToEntities(dbQuery)
  }

  // Find all configurations that have one of the specified method ids.
  override def findConfigurations(ids: Seq[ObjectId]) = {
    val dbQuery = "methodId" $in ids
    queryToEntities(dbQuery)
  }

  private def queryToEntities(dbQuery: DBObject) = {
    val entityCollections = getCollectionsByEntityType(Option(AgoraEntityType.Configuration))
    val entities = entityCollections.flatMap {
      collection =>
        collection.find(dbQuery).map {
          dbObject => MongoDbObjectToEntity(dbObject)
        }
    }

    entities
  }

  def assureSingleCollection: MongoCollection = {
    if (collections.size != 1) throw new IllegalArgumentException("Multiple collections defined. Only a single collection is supported for this operation")
    else collections.head
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

  // Populate method field within configurations
  def addMethodRef(entities: Seq[AgoraEntity], projection: Option[AgoraEntityProjection]): Seq[AgoraEntity] = {
    // Don't bother trying to populate method refs unless there are configs in the result set (only configs have embedded methods)
    val configCollection = getCollectionsByEntityType(Seq(AgoraEntityType.Configuration)).head
    if (!collections.contains(configCollection)) {
      return entities
    }

    // Get map of method ids to Methods
    val methodRefs = methodIds(entities)
    val methodCollections = getCollectionsByEntityType(AgoraEntityType.MethodTypes)
    val methods = findById(methodRefs, methodCollections, projection)
    val methodsMap = idToEntityMap(methods, methodCollections)

    // Populate method field if methodId has a value
    val entitiesWithRefs = entities.map {
      entity =>
        if (entity.methodId.nonEmpty) {
          val method = methodsMap.get(entity.methodId.get)
          entity.addMethod(Option(method.get.addUrl().removeIds()))
        }
        else {
          entity
        }
    }
    entitiesWithRefs
  }

  def methodIds(entities: Seq[AgoraEntity]): Seq[ObjectId] = entities.flatMap { entity => entity.methodId }

  def idToEntityMap(entities: Seq[AgoraEntity], entityCollections: Seq[MongoCollection]): Map[ObjectId, AgoraEntity] = {
    entities.map { entity => entity.id.get -> entity }.toMap
  }
}
