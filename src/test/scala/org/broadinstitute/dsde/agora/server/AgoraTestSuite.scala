package org.broadinstitute.dsde.agora.server

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraAuthorizationTest, RoleTranslatorTest}
import org.broadinstitute.dsde.agora.server.dataaccess.authorization.TestAuthorizationProvider
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{AgoraMongoClient, MethodsDbTest}
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupportTest, AgoraEntityTest}
import org.broadinstitute.dsde.agora.server.webservice._
import org.scalatest.{BeforeAndAfterAll, Suites}

trait AgoraDbTest {
  val mongoTestCollection = AgoraMongoClient.getCollection("test")
  val agoraDao = AgoraDao.createAgoraDao(mongoTestCollection)
}

class AgoraTestSuite extends Suites(
  new AgoraMethodsSpec,
  new AgoraProjectionsSpec,
  new AgoraImportSpec,
  new AgoraConfigurationsSpec,
  new MethodsDbTest,
  new AgoraBusinessTest,
  new AgoraApiJsonSupportTest,
  new AgoraEntityTest,
  new AgoraAuthorizationTest,
  new RoleTranslatorTest) with AgoraTestData with BeforeAndAfterAll with MongoEmbedDatabase {

  val agora = new Agora(TestAuthorizationProvider)
  val agoraBusiness = new AgoraBusiness()
  var mongoProps: MongodProps = null

  override def beforeAll() {
    println("Starting embedded mongo db instance.")
    mongoProps = mongoStart(port = AgoraConfig.mongoDbPort)
    println("Starting Agora web services.")

    agora.start(TestAuthorizationProvider)
    agoraBusiness.insert(testEntity1, agoraCIOwner.get)
    agoraBusiness.insert(testEntity2, agoraCIOwner.get)
    agoraBusiness.insert(testEntity3, agoraCIOwner.get)
    agoraBusiness.insert(testEntity4, agoraCIOwner.get)
    agoraBusiness.insert(testEntity5, agoraCIOwner.get)
    agoraBusiness.insert(testEntity6, agoraCIOwner.get)
    agoraBusiness.insert(testEntity7, agoraCIOwner.get)
    agoraBusiness.insert(testEntityTaskWc, agoraCIOwner.get)
    agoraBusiness.insert(testAgoraConfigurationEntity, agoraCIOwner.get)
  }

  override def afterAll() {
    println("Stopping embedded mongo db instance.")
    mongoStop(mongoProps)
    println("Stopping Agora web services.")
    agora.stop()
  }
}
