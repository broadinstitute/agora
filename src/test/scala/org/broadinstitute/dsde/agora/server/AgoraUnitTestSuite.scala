package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraAuthorizationTest, RoleTranslatorTest}
import org.broadinstitute.dsde.agora.server.dataaccess.authorization.TestAuthorizationProvider
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

  val agora = new Agora(TestAuthorizationProvider)
  val agoraBusiness = new AgoraBusiness(TestAuthorizationProvider)

  override def beforeAll() {
    EmbeddedMongo.startMongo()
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
    EmbeddedMongo.stopMongo()
  }
}