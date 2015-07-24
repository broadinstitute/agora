
package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.dataaccess.acls.GcsAuthorizationSpec
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsAuthorizationProvider
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.EmbeddedMongo
import org.broadinstitute.dsde.agora.server.webservice.AclIntegrationSpec
import org.scalatest.{BeforeAndAfterAll, Suites}

class AgoraIntegrationTestSuite extends Suites(
  new GcsAuthorizationSpec,
  new AclIntegrationSpec) with BeforeAndAfterAll {
  val agora = new Agora(AgoraConfig.DevEnvironment)

  override def beforeAll() {
    println(s"Starting Agora web services ($suiteName)")
    agora.start()
    EmbeddedMongo.startMongo()
  }

  override def afterAll() {
    println(s"Stopping Agora web services ($suiteName)")
    agora.stop()
    EmbeddedMongo.stopMongo()
  }
}
