
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.client.model.{Filters, Projections, Updates}
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoDao._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.exceptions.AgoraEntityNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityProjection._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityBsonSupport, AgoraEntityProjection, AgoraEntityType}
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.bson._
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

object AgoraMongoDao {
  val MongoDbIdField = "_id"
  val CounterSequenceField = "seq"
  val KeySeparator = ":"
  val DefaultFindProjection: Option[AgoraEntityProjection] =
    Option(new AgoraEntityProjection(Seq.empty[String], Seq[String]("payload", "documentation")))

  def EntityToMongoDbObject(entity: AgoraEntity): Document = {
    //we need to remove url if it exists since we don't store it in the document
    Document(entity.copy(url = None).toJson.toString())
  }

  def MongoDbObjectToEntity(mongoDBObject: Document): AgoraEntity = {
    AgoraEntityBsonSupport.read(mongoDBObject)
  }
}

/**
 * The data access object for agora's mongo database. If more than one collection is specified we will query all
 * collections. However, only a single collection is allowed for doing insert operations.
 * @param collections The collections to query. In order to insert a single colleciton must be specified.
 */
class AgoraMongoDao(collections: Seq[MongoCollection[Document]]) extends AgoraDao with LazyLogging {

  /**
   * On insert we query for the given namespace/name if it exists we increment the snapshotId and store a new one.
   * @param entity The entity to store.
   * @return The entity that was stored.
   */
  override def insert(entity: AgoraEntity)(implicit executionContext: ExecutionContext): Future[AgoraEntity] = {
    val collection = assureSingleCollection
    for {
      //update the snapshotId
      id <- getNextId(entity)
      entityWithId = entity.copy(snapshotId = Option(id))

      //insert the entity
      dbEntityToInsert = EntityToMongoDbObject(entityWithId)
      _ <- collection.insertOne(dbEntityToInsert).head()
      insertedEntity <- findSingle(MongoDbObjectToEntity(dbEntityToInsert))
    } yield insertedEntity
  }

  override def findSingle(namespace: String, name: String, snapshotId: Int)
                         (implicit executionContext: ExecutionContext): Future[AgoraEntity] = {
    val entity = AgoraEntity(namespace = Option(namespace), name = Option(name), snapshotId = Option(snapshotId))
    findSingle(entity)
  }

  override def findSingle(entity: AgoraEntity)(implicit executionContext: ExecutionContext): Future[AgoraEntity] = {
    val dbEntity = EntityToMongoDbObject(entity)
    val entityVectorFuture = find(dbEntity, None)
    entityVectorFuture flatMap { entityVector =>
      entityVector.length match {
        case 1 =>
          val foundEntity = entityVector.head
          addMethodRef(Seq(foundEntity), None).map(_.head)
        case 0 => Future.failed(AgoraEntityNotFoundException(entity))
        case _ => Future.failed(new Exception("Found > 1 documents matching: " + entity.toString))
      }
    }
  }

  override def find(entity: AgoraEntity, projectionOpt: Option[AgoraEntityProjection])
                   (implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]] = {
    val projection = projectionOpt match {
      case Some(_) => projectionOpt
      case None => DefaultFindProjection
    }
    val dbEntity = EntityToMongoDbObject(entity)
    val entitiesFuture = find(dbEntity, projection)
    entitiesFuture.flatMap(entities => addMethodRef(entities, projection))
  }

  def findById(ids: Seq[ObjectId],
               entityCollections: Seq[MongoCollection[Document]],
               projection: Option[AgoraEntityProjection]
              )
              (implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]] = {
    val dbQuery = Filters.in(MongoDbIdField, ids:_*)
    val entities = entityCollections.map {
      collection =>
        collection.find(dbQuery).projection(projectionToDBProjections(projection).orNull).map {
          dbObject => MongoDbObjectToEntity(dbObject)
        }.toFuture()
    }
    Future.sequence(entities).map(_.flatten)
  }

  // Find all configurations that have specified method id.
  override def findConfigurations(id: ObjectId)
                                 (implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]] = {
    val dbQuery = Filters.eq("methodId", id)
    queryToEntities(dbQuery)
  }

  // Find all configurations that have one of the specified method ids.
  override def findConfigurations(ids: Seq[ObjectId])
                                 (implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]] = {
    val dbQuery = Filters.in("methodId", ids: _*)
    queryToEntities(dbQuery)
  }

  private def queryToEntities(dbQuery: Bson)(implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]] = {
    val entityCollections = getCollectionsByEntityType(Option(AgoraEntityType.Configuration))
    val entities = entityCollections.map {
      collection =>
        collection.find(dbQuery).map {
          dbObject => MongoDbObjectToEntity(dbObject)
        }.toFuture()
    }

    Future.sequence(entities).map(_.flatten)
  }

  def assureSingleCollection: MongoCollection[Document] = {
    if (collections.size != 1) throw new IllegalArgumentException("Multiple collections defined. Only a single collection is supported for this operation")
    else collections.head
  }

  def getNextId(entity: AgoraEntity)(implicit executionContext: ExecutionContext): Future[Int] = {
    //first check to see if we have a sequence
    val counterCollection = getCountersCollection
    val counterId: String = entity.namespace.get + KeySeparator + entity.name.get
    val counterQuery = Filters.eq(MongoDbIdField, counterId)

    for {
      //if we don't have a sequence create one
      maybeCounter <- counterCollection.find(counterQuery).first().headOption()
      _ <- if (maybeCounter.isEmpty) {
        counterCollection.insertOne(Document(MongoDbIdField -> counterId, CounterSequenceField -> 0)).toFuture()
      } else {
        Future.successful(())
      }
      //find and modify the sequence
      currentCount <-
        counterCollection
          .findOneAndUpdate(
            counterQuery,
            Updates.inc(CounterSequenceField, 1),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
          )
          .toFuture()
    } yield
      //return new sequence
      currentCount[BsonInt32](CounterSequenceField).getValue
  }

  def find(query: Bson, projection: Option[AgoraEntityProjection])
          (implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]] = {
    val seqOfFutureSeq: Seq[Future[Seq[AgoraEntity]]] = collections.map {
      collection =>
        collection.find(query).projection(projectionToDBProjections(projection).orNull).map {
          dbObject =>
            MongoDbObjectToEntity(dbObject)
        }.toFuture()
    }

    Future.sequence(seqOfFutureSeq).map(_.flatten)
  }

  def projectionToDBProjections(projectionOpt: Option[AgoraEntityProjection]): Option[Bson] = {
    projectionOpt map {
      projection =>
        //we can't currently mix excluded and included fields This is a limitation of mongo.
        if (projection.excludedFields.isEmpty) {
          Projections.include(projection.includedFields ++ RequiredProjectionFields:_*)
        } else {
          Projections.exclude(projection.excludedFields:_*)
        }
    }
  }

  // Populate method field within configurations
  def addMethodRef(entities: Seq[AgoraEntity], projection: Option[AgoraEntityProjection])
                  (implicit executionContext: ExecutionContext): Future[Seq[AgoraEntity]] = {
    // Don't bother trying to populate method refs unless there are configs in the result set (only configs have embedded methods)
    val configCollection = getCollectionsByEntityType(Seq(AgoraEntityType.Configuration)).head
    if (!collections.map(_.namespace).contains(configCollection.namespace)) {
      return Future.successful(entities)
    }

    // Get map of method ids to Methods
    val methodRefs = methodIds(entities)
    val methodCollections = getCollectionsByEntityType(AgoraEntityType.MethodTypes)
    val methodsFuture = findById(methodRefs, methodCollections, projection)
    val methodsMapFuture = methodsFuture.map(methods => idToEntityMap(methods))

    // Populate method field if methodId has a value
    val entitiesWithRefs = methodsMapFuture.map(methodsMap => entities.map {
      entity =>
        if (entity.methodId.nonEmpty) {
          val method = methodsMap.get(entity.methodId.get)
          entity.addMethod(Option(method.get.addUrl().removeIds()))
        }
        else {
          entity
        }
    })
    entitiesWithRefs
  }

  def methodIds(entities: Seq[AgoraEntity]): Seq[ObjectId] = entities.flatMap { entity => entity.methodId }

  def idToEntityMap(entities: Seq[AgoraEntity]): Map[ObjectId, AgoraEntity] = {
    entities.map { entity => entity.id.get -> entity }.toMap
  }
}
