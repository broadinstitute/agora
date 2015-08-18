
package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.business.AgoraBusinessIntegrationSpec
import org.broadinstitute.dsde.agora.server.webservice.AgoraImportIntegrationSpec
import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.EmbeddedMongo
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.webservice.PermissionIntegrationSpec
import org.scalatest.{BeforeAndAfterAll, Suites}
import slick.driver.MySQLDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration._

class AgoraIntegrationTestSuite extends Suites(
  new AgoraBusinessIntegrationSpec,
  new AgoraImportIntegrationSpec,
  new PermissionIntegrationSpec) with BeforeAndAfterAll {
  val agora = new Agora()

  val timeout = 10.seconds
  var db: Database = _

  override def beforeAll() {
    println(s"Starting Agora web services ($suiteName)")
    agora.start()
    EmbeddedMongo.startMongo()

    db = AgoraConfig.sqlDatabase

    val setupFuture = db.run(
      (entities.schema ++
        users.schema ++
        permissions.schema).create
    )

    println("Connecting to sql database.")
    Await.result(setupFuture, timeout)

  }

  override def afterAll() {
    println(s"Stopping Agora web services ($suiteName)")
    agora.stop()
    EmbeddedMongo.stopMongo()

    val tearDownFuture = db.run(
      (entities.schema ++
        users.schema ++
        permissions.schema).drop
    )
    Await.ready(tearDownFuture, timeout)
    println("Disconnecting from sql database.")
    db.close

  }
}
