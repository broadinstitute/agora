package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.{AgoraDbTest, AgoraTestData}
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class MethodsDbTest extends FlatSpec with AgoraDbTest with AgoraTestData {

  "Agora" should "be able to store a method" in {
    val entityWithId = agoraDao.insert(testEntity1)

    val entity = agoraDao.findSingle(entityWithId).get

    assert(entity == entityWithId)
  }

  "Agora" should "be able to query by namespace, name and snapshot id (version) and get back a single entity" in {
    val entityWithId = agoraDao.insert(testEntity1)
    val queryEntity = new AgoraEntity(namespace = namespace1, name = name1, snapshotId = entityWithId.snapshotId)

    val entity = agoraDao.findSingle(queryEntity).get

    assert(entity == entityWithId)
  }

  "Agora" should "increment the snapshot id number if we insert the same namespace/name entity" in {
    val entityWithId = agoraDao.insert(testEntity1)
    val entityWithId2 = agoraDao.insert(testEntity1)

    val previousVersionEntity = entityWithId.copy(snapshotId = entityWithId2.snapshotId.map(id => id - 1))

    val entity1 = agoraDao.findSingle(previousVersionEntity).get
    val entity2 = agoraDao.findSingle(entityWithId2).get

    assert(entity1.snapshotId.get == entity2.snapshotId.get - 1)
  }

  "Agora" should "not find an entity if it doesn't exist" in {
    val entity = agoraDao.findSingle(testAgoraEntityNonExistent)
    assert(entity.isEmpty)
  }
}
