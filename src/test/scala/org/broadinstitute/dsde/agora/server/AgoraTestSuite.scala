package org.broadinstitute.dsde.agora.server

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import com.mongodb.casbah.MongoClient
import org.broadinstitute.dsde.agora.server.business.{AgoraAccessControlTest, AgoraBusiness, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{AgoraMongoClient, MethodsDbTest}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupportTest
import org.broadinstitute.dsde.agora.server.webservice._
import org.broadinstitute.dsde.agora.server.webservice.validation.AgoraValidationTest
import org.scalatest.{BeforeAndAfterAll, Suites}

trait AgoraDbTest {
  val mongoTestCollection = AgoraMongoClient.getCollection(MongoClient().getDB("agora"), "test")
  val agoraDao = AgoraDao.createAgoraDao(mongoTestCollection)
}

class AgoraTestSuite extends Suites(
  new AgoraMethodsSpec,
  new AgoraProjectionsSpec,
  new AgoraReferencesSpec,
  new AgoraConfigurationsSpec,
  new MethodsDbTest,
  new AgoraBusinessTest,
  new AgoraValidationTest,
  new AgoraApiJsonSupportTest,
  new AgoraAccessControlTest) with AgoraTestData with BeforeAndAfterAll with MongoEmbedDatabase {

  var mongoProps: MongodProps = null

  override def beforeAll() {
    println("Starting embedded mongo db instance.")
    mongoProps = mongoStart(port = AgoraConfig.mongoDbPort)
    println("Starting Agora web services.")
    Agora.start()

    AgoraBusiness.insert(testEntity1)
    AgoraBusiness.insert(testEntity2)
    AgoraBusiness.insert(testEntity3)
    AgoraBusiness.insert(testEntity4)
    AgoraBusiness.insert(testEntity5)
    AgoraBusiness.insert(testEntity6)
    AgoraBusiness.insert(testEntity7)

    AgoraBusiness.insert(testEntityTaskWc)
    AgoraBusiness.insert(testAgoraConfigurationEntity)
  }

  override def afterAll() {
    println("Stopping embedded mongo db instance.")
    mongoStop(mongoProps)
    println("Stopping Agora web services.")
    Agora.stop()
  }
}
