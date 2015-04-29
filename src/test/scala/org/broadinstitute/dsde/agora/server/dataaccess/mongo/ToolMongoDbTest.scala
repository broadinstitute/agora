package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import java.util.Date

import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraMetadata}
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

class ToolMongoDbTest extends FlatSpec with BeforeAndAfterAll {

  val agoraToolOne: AgoraEntity = new AgoraEntity( new AgoraMetadata(namespace = "broadinstitute", name="echo",
    synopsis = "The is a tool that echoes a string to standard out",
    documentation = "This string a quite a bit longer than the synopsis as it should contain the documenation for the tool (perhaps using mark(up/down) language",
    owner = "jcarey", createDate = new Date()), payload = "{\"executable\":\"echo\"}")

  val agoraToolTwo: AgoraEntity = new AgoraEntity( new AgoraMetadata(namespace = "broadinstitute", name="echo",
    synopsis = "The is a tool that echoes a string to standard out",
    documentation = "This string a quite a bit longer than the synopsis as it should contain the documenation for the tool (perhaps using mark(up/down) language",
    owner = "jcarey", createDate = new Date()), payload = "{\"executable\":\"echo2\"}")

  def fixture =
    new {
      val toolCollection = getCollection(getDatabase(MongoClient(), "methods"), "tools")
      val agoraDao = new AgoraMongoDao(toolCollection)
    }

  override def beforeAll() = {
    val testFixture = fixture
    testFixture.toolCollection.remove(MongoDBObject())
    testFixture.toolCollection.drop()
  }

  "The method store" should "be able to store a tool" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraToolOne)

    val entities = testFixture.agoraDao.findByName("echo")

    assert(entities.size == 1)
    assert(entities.toVector.head == agoraToolOne)
  }

  "The method store" should "be able to query by namespace, name and version and get back a single entity" in {
    val testFixture = fixture

    val entity = testFixture.agoraDao.find(namespace = "broadinstitute", name = "echo", id = agoraToolOne.metadata.id.get)

    assert(entity == agoraToolOne)
  }

  "The method store" should "increment the id number if we insert the same namespace/name entity" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraToolOne)

    val entity1 = testFixture.agoraDao.find("broadinstitute", "echo", agoraToolOne.metadata.id.get - 1)
    val entity2 = testFixture.agoraDao.find("broadinstitute", "echo", agoraToolOne.metadata.id.get)

    assert(entity1.metadata.id != entity2.metadata.id)
  }

  "The method store" should "be able to query by a string in the entity payload" in {
    val testFixture = fixture

    testFixture.agoraDao.insert(agoraToolTwo)

    val entities = testFixture.agoraDao.findPayloadByRegex(".*echo2.*")

    assert(entities.size == 1)
    assert(entities.toVector.head == agoraToolTwo)
  }
}
