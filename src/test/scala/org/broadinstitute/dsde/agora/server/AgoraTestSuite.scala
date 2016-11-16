package org.broadinstitute.dsde.agora.server

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.business.{AgoraBusinessIntegrationSpec, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.MethodsDbTest
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupportTest, AgoraEntityTest}
import org.broadinstitute.dsde.agora.server.webservice._
import org.scalatest.{BeforeAndAfterAll, Suites}

class AgoraTestSuite extends Suites(
  new AgoraServiceStatusSpec,
  new AgoraMethodsSpec,
  new AgoraProjectionsSpec,
  new AgoraImportSpec,
  new AgoraConfigurationsSpec,
  new MethodsDbTest,
  new AgoraBusinessTest,
  new AgoraApiJsonSupportTest,
  new AgoraEntityTest,
  new AgoraPermissionsSpec,
  new EntityPermissionsClientSpec,
  new NamespacePermissionsClientSpec,
  new ManagerPermissionSpec,
  new AgoraBusinessIntegrationSpec,
  new AgoraImportIntegrationSpec,
  new PermissionIntegrationSpec,
  new AdminSweeperSpec(ActorSystem("test"))) with BeforeAndAfterAll with AgoraTestFixture {

  val agora = new Agora()


  override def beforeAll() {
    startDatabases()
    println(s"Starting Agora web services ($suiteName)")
    agora.start()
  }


  override def afterAll() {
    stopDatabases()
    println(s"Stopping Agora web services ($suiteName)")
    agora.stop()
  }
}
