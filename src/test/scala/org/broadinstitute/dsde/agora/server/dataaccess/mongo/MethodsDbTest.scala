package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.exceptions.AgoraEntityNotFoundException
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.{AgoraScalaFutures, AgoraTestFixture}
import org.mongodb.scala.{Document, MongoCollection}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover}

@DoNotDiscover
class MethodsDbTest extends AnyFlatSpec with BeforeAndAfterAll with AgoraTestFixture with AgoraScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  /*
  NOTE: This must be lazy! Due to a large number of Agora singletons including `object AgoraMongoClient` one must wait
  for `beforeAll()` defined below to run before trying to access singleton methods. `ensureDatabasesAreRunning()` sets
  an internal variable in the `object AgoraMongoClient` singleton that points to the correct TestContainer.
   */
  lazy val mongoTestCollection: MongoCollection[Document] = AgoraMongoClient.getCollection("test")
  lazy val agoraDao: AgoraDao = AgoraDao.createAgoraDao(mongoTestCollection)

  override protected def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
  }

  override protected def afterAll(): Unit = {
    clearDatabases()
    clearMongoCollections(Seq(mongoTestCollection))
  }

  "Agora" should "be able to store a method" in {
    val entityWithId = agoraDao.insert(testEntity1).futureValue

    val entity = agoraDao.findSingle(entityWithId).futureValue

    assert(entity == entityWithId)
  }

  "Agora" should "be able to query by namespace, name and snapshot id (version) and get back a single entity" in {
    val entityWithId = agoraDao.insert(testEntity1).futureValue
    val queryEntity = new AgoraEntity(namespace = namespace1, name = name1, snapshotId = entityWithId.snapshotId, entityType = Option(AgoraEntityType.Workflow))

    val entity = agoraDao.findSingle(queryEntity).futureValue

    assert(entity == entityWithId)
  }

  "Agora" should "increment the snapshot id number if we insert the same namespace/name entity" in {
    val entityWithId = agoraDao.insert(testEntity1).futureValue
    val entityWithId2 = agoraDao.insert(testEntity1).futureValue

    val previousVersionEntity = entityWithId.copy(snapshotId = entityWithId2.snapshotId.map(id => id - 1))

    val entity1 = agoraDao.findSingle(previousVersionEntity).futureValue
    val entity2 = agoraDao.findSingle(entityWithId2).futureValue

    assert(entity1.snapshotId.get == entity2.snapshotId.get - 1)
  }

  "Agora" should "not find an entity if it doesn't exist" in {
    val thrown = intercept[AgoraEntityNotFoundException] {
      ScalaFutures.whenReady(agoraDao.findSingle(testAgoraEntityNonExistent).failed)(throwable => throw throwable)
    }
    assert(thrown != null)
  }
}
