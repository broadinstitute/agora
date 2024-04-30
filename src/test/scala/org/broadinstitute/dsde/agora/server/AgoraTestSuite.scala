package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.business.{AgoraBusinessIntegrationSpec, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.MethodsDbTest
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntityTest, Ga4ghModelTest}
import org.broadinstitute.dsde.agora.server.webservice._
import org.scalatest.{BeforeAndAfterAll, Suites}

class AgoraTestSuite extends Suites(
  new AgoraServiceUnhealthyStatusSpec,
  new AgoraServiceHealthyStatusSpec,
  new AgoraMethodsSpec,
  new DockerHubClientSpec,
  new EntityCreationPermissionSpec,
  new EntityCopySpec,
  new AgoraProjectionsSpec,
  new AgoraConfigurationsSpec,
  new MethodsDbTest,
  new AgoraBusinessTest,
  new AgoraEntityTest,
  new AgoraPermissionsSpec,
  new EntityPermissionsClientSpec,
  new NamespacePermissionsClientSpec,
  new AgoraBusinessIntegrationSpec,
  new AgoraImportIntegrationSpec,
  new PermissionIntegrationSpec,
  new MethodDefinitionIntegrationSpec,
  new AssociatedConfigurationIntegrationSpec,
  new CompatibleConfigurationIntegrationSpec,
  new Ga4ghServiceSpec,
  new Ga4ghModelTest,
  new AdminSweeperSpec)
  with BeforeAndAfterAll with AgoraTestFixture {

  val agora = new Agora()

  override def beforeAll(): Unit = {
    startDatabases()
    println(s"Starting Agora web services ($suiteName)")
    agora.start()
  }
  
  override def afterAll(): Unit = {
    stopDatabases()
    println(s"Stopping Agora web services ($suiteName)")
    agora.stop()
  }
}
