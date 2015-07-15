package org.broadinstitute.dsde.agora.server

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import de.flapdoodle.embed.mongo.distribution.Version
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraAuthorizationTest, RoleTranslatorTest}
import org.broadinstitute.dsde.agora.server.dataaccess.authorization.TestAuthorizationProvider
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.MethodsDbTest
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupportTest, AgoraEntityTest}
import org.broadinstitute.dsde.agora.server.webservice._
import org.scalatest.{BeforeAndAfterAll, Suites}

object EmbeddedMongoDb extends MongoEmbedDatabase {
  var mongodProps: MongodProps = null

  def startMongo() = {
    println(s"Starting embedded mongo db instance.")
    if (mongodProps == null || !mongodProps.mongodProcess.isProcessRunning) {
      mongodProps = mongoStart(port = AgoraConfig.mongoDbPort, version = Version.V2_7_1)
    }
  }

  def stopMongo() = {
    println(s"Stopping embedded mongo db instance.")
    mongoStop(mongodProps)
    //for some unknown reason the mongo db needs to wait a bit before it can be started back up
    //this is likely a java driver issue and should be revisited when the driver is updated.
    Thread.sleep(5000)
  }
}

class AgoraUnitTestSuite extends Suites(
  new AgoraMethodsSpec,
  new AgoraProjectionsSpec,
  new AgoraImportSpec,
  new AgoraConfigurationsSpec,
  new MethodsDbTest,
  new AgoraBusinessTest,
  new AgoraApiJsonSupportTest,
  new AgoraEntityTest,
  new AgoraAuthorizationTest,
  new RoleTranslatorTest) with BeforeAndAfterAll {

  val agora = new Agora(TestAuthorizationProvider)
  val agoraBusiness = new AgoraBusiness(TestAuthorizationProvider)

  override def beforeAll() {
    EmbeddedMongoDb.startMongo()
    println(s"Starting Agora web services ($suiteName)")
    agora.start()

    TestAuthorizationProvider.createEntityAuthorizations(testEntity1, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testEntity2, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testEntity3, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testEntity4, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testEntity5, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testEntity6, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testEntity7, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testEntityTaskWc, agoraCIOwner.get)
    TestAuthorizationProvider.createEntityAuthorizations(testAgoraConfigurationEntity, agoraCIOwner.get)

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
    println(s"Stopping Agora web services ($suiteName)")
    agora.stop()
    EmbeddedMongoDb.stopMongo()
  }
}