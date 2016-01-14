package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.exceptions.AgoraEntityNotFoundException
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}

@DoNotDiscover
class MethodsDbTest extends FlatSpec with BeforeAndAfterAll with AgoraTestFixture {
  val mongoTestCollection = AgoraMongoClient.getCollection("test")
  val agoraDao = AgoraDao.createAgoraDao(mongoTestCollection)

  override protected def beforeAll() = {
    ensureMongoDatabaseIsRunning()
  }

  override protected def afterAll() = {
    clearMongoCollections(Seq(mongoTestCollection))
  }

  "Agora" should "be able to store a method" in {
    val entityWithId = agoraDao.insert(testWorkflow1)

    val entity = agoraDao.findSingle(entityWithId)

    assert(entity == entityWithId)
  }

  "Agora" should "be able to query by namespace, name and snapshot id (version) and get back a single entity" in {
    val entityWithId = agoraDao.insert(testWorkflow1)
    val queryEntity = new AgoraEntity(namespace = namespace1, name = name1, snapshotId = entityWithId.snapshotId, entityType = Option(AgoraEntityType.Workflow))

    val entity = agoraDao.findSingle(queryEntity)

    assert(entity == entityWithId)
  }

  "Agora" should "increment the snapshot id number if we insert the same namespace/name entity" in {
    val entityWithId = agoraDao.insert(testWorkflow1)
    val entityWithId2 = agoraDao.insert(testWorkflow1)

    val previousVersionEntity = entityWithId.copy(snapshotId = entityWithId2.snapshotId.map(id => id - 1))

    val entity1 = agoraDao.findSingle(previousVersionEntity)
    val entity2 = agoraDao.findSingle(entityWithId2)

    assert(entity1.snapshotId.get == entity2.snapshotId.get - 1)
  }

  "Agora" should "not find an entity if it doesn't exist" in {
    val thrown = intercept[AgoraEntityNotFoundException] {
      agoraDao.findSingle(testNonExistentWorkflow)
    }
    assert(thrown != null)
  }
}
