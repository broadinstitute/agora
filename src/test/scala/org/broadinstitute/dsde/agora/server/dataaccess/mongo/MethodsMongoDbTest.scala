package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import java.util.Date

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraMetadata}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

class MethodsMongoDbTest extends FlatSpec with BeforeAndAfterAll {

  val agoraMethodOne: AgoraEntity = new AgoraEntity(new AgoraMetadata(namespace = "broadinstitute", name = "echo",
    synopsis = "The is a method that echoes a string to standard out",
    documentation = "This string a quite a bit longer than the synopsis as it should contain the documenation for the method (perhaps using mark(up/down) language",
    owner = "jcarey", createDate = new Date()), payload = "{\"executable\":\"echo\"}")

  val agoraMethodTwo: AgoraEntity = new AgoraEntity(new AgoraMetadata(namespace = "broadinstitute", name = "echo",
    synopsis = "The is a method that echoes a string to standard out",
    documentation = "This string a quite a bit longer than the synopsis as it should contain the documenation for the method (perhaps using mark(up/down) language",
    owner = "jcarey", createDate = new Date()), payload = "{\"executable\":\"echo2\"}")

  def fixture =
    new {
      val methodsCollection = getMethodsCollection(MongoClient())
      val agoraDao = new AgoraMongoDao(methodsCollection)
    }

  override def beforeAll() = {
    val testFixture = fixture
    testFixture.methodsCollection.remove(MongoDBObject())
    testFixture.methodsCollection.drop()
  }

  "Agora" should "be able to store a method" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraMethodOne)

    val entities = testFixture.agoraDao.findByName("echo")

    assert(entities.size == 1)
    assert(entities.toVector.head == agoraMethodOne)
  }

  "Agora" should "be able to query by namespace, name and version and get back a single entity" in {
    val testFixture = fixture

    val entity = testFixture.agoraDao.find(namespace = "broadinstitute", name = "echo", id = agoraMethodOne.metadata.id.get)

    assert(entity == agoraMethodOne)
  }

  "Agora" should "increment the id number if we insert the same namespace/name entity" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraMethodOne)

    val entity1 = testFixture.agoraDao.find("broadinstitute", "echo", agoraMethodOne.metadata.id.get - 1)
    val entity2 = testFixture.agoraDao.find("broadinstitute", "echo", agoraMethodOne.metadata.id.get)

    assert(entity1.metadata.id != entity2.metadata.id)
  }

  "Agora" should "be able to query by a string in the entity payload" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraMethodTwo)

    val entities = testFixture.agoraDao.findPayloadByRegex(".*echo2.*")

    assert(entities.size == 1)
    assert(entities.toVector.head == agoraMethodTwo)
  }
}
