package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessTest}
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{EmbeddedMongo, MethodsDbTest}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupportTest, AgoraEntityTest}
import org.broadinstitute.dsde.agora.server.webservice._
import org.scalatest.{BeforeAndAfterAll, Suites}
import slick.driver.MySQLDriver.api._


import scala.concurrent.Await
import scala.concurrent.duration._

class AgoraUnitTestSuite extends Suites(
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
  new NamespacePermissionsClientSpec) with BeforeAndAfterAll {

  val agora = new Agora()     // Unit Tests use the local environment settings
  val agoraBusiness = new AgoraBusiness()
  val timeout = 10.seconds

  var db: Database = _

  override def beforeAll() {
    EmbeddedMongo.startMongo()
    println(s"Starting Agora web services ($suiteName)")
    agora.start()

    println("Connecting to test sql database.")
    db = AgoraConfig.sqlDatabase

    val setupFuture = DBIO.seq(
      (entities.schema ++
        users.schema ++
        permissions.schema).create,

      users += UserDao(adminUser.get, isAdmin = true)
    )

    println("Populating databases.")
    Await.result(db.run(setupFuture), timeout)

    agoraBusiness.insert(testEntity1, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity3, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity4, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity5, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity6, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity7, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntityTaskWc, mockAutheticatedOwner.get)
    agoraBusiness.insert(testAgoraConfigurationEntity, mockAutheticatedOwner.get)
    agoraBusiness.insert(testAgoraConfigurationEntity2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntityToBeRedacted, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntityToBeRedacted2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntityToBeRedacted3, mockAutheticatedOwner.get)
    agoraBusiness.insert(testAgoraConfigurationToBeRedacted, mockAutheticatedOwner.get)
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
