package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{MockAuthorizationProvider, AgoraAuthorizationTest, RoleTranslatorTest}
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{EmbeddedMongo, MethodsDbTest}
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupportTest, AgoraEntityTest}
import org.broadinstitute.dsde.agora.server.webservice._
import org.scalatest.{BeforeAndAfterAll, Suites}

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

  val agora = new Agora(AgoraConfig.LocalEnvironment)     // Unit Tests use the local environment settings
                                                          // mockAuthentication, mockAuthorization, embedded Mongo
  val agoraBusiness = new AgoraBusiness(MockAuthorizationProvider)

  override def beforeAll() {
    EmbeddedMongo.startMongo()
    println(s"Starting Agora web services ($suiteName)")
    agora.start()

    MockAuthorizationProvider.createEntityAuthorizations(testEntity1, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testEntity2, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testEntity3, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testEntity4, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testEntity5, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testEntity6, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testEntity7, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testEntityTaskWc, agoraCIOwner.get)
    MockAuthorizationProvider.createEntityAuthorizations(testAgoraConfigurationEntity, agoraCIOwner.get)

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
    EmbeddedMongo.stopMongo()
  }
}