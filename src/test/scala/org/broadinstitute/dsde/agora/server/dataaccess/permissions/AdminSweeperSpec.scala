package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper.Sweep
import org.broadinstitute.dsde.agora.server.{AgoraTestData, AgoraTestFixture}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

object AdminSweeperSpec {
  def getMockAdminsList: () => List[String] = { () =>
    List("fake@broadinstitute.org", AgoraTestData.owner1.get, AgoraTestData.owner2.get)
  }
}

class AdminSweeperSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers with BeforeAndAfterAll with AgoraTestFixture with ImplicitSender {

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  "Agora" should {
    "be able to synchronize it's list of admins via the AdminSweeper" in {
      addAdminUser()

      val adminSweeper = TestActorRef(AdminSweeper.props(AdminSweeperSpec.getMockAdminsList))
      within(30 seconds) {
        adminSweeper ! Sweep
        awaitAssert(AdminPermissionsClient.listAdminUsers === AdminSweeperSpec.getMockAdminsList)
      }
    }

  }

}
