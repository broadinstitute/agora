package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import java.util.Date

import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

class MethodsDbTest extends FlatSpec with BeforeAndAfterAll {

  val agoraTestMethod: AgoraEntity = new AgoraEntity(
    namespace = Option("broadinstitute"),
    name = Option("echo"),
    synopsis = Option("The is a method that echoes a string to standard out"),
    documentation = Option("This string a quite a bit longer than the synopsis as it should contain the documentation for the method (perhaps using mark(up/down) language"),
    owner = Option("jcarey"),
    createDate = Option(new Date()),
    payload = Option("{\"executable\":\"echo\"}")
  )

  def fixture =
    new {
      val agoraDao = AgoraDao.createAgoraDao
    }
  
  "Agora" should "be able to store a method" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraTestMethod)
    
    val entity = testFixture.agoraDao.findSingle(agoraTestMethod).get

    assert(entity == agoraTestMethod)
  }

  "Agora" should "be able to query by namespace, name and version and get back a single entity" in {
    val testFixture = fixture

    //NB: agoraTestMethod has already been stored.
    val queryEntity = new AgoraEntity(namespace = Option("broadinstitute"), name = Option("echo"), id = agoraTestMethod.id)

    val entity = testFixture.agoraDao.findSingle(queryEntity).get

    assert(entity == agoraTestMethod)
  }

  "Agora" should "increment the id number if we insert the same namespace/name entity" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraTestMethod)

    val previousVersionEntity = agoraTestMethod.copy()
    previousVersionEntity.id = Option(agoraTestMethod.id.get - 1)

    val entity1 = testFixture.agoraDao.findSingle(previousVersionEntity).get
    val entity2 = testFixture.agoraDao.findSingle(agoraTestMethod).get

    assert(entity1.id.get == entity2.id.get - 1)
  }
}