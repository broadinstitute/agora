package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestActorRef}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper.Sweep
import org.broadinstitute.dsde.agora.server.{AgoraTestData, AgoraTestFixture}
import org.scalatest.{DoNotDiscover, WordSpecLike, Matchers, BeforeAndAfterAll}
import scala.concurrent.duration._

object AdminSweeperSpec {
  def getMockAdminsList:() =>  List[String] = { () =>
    List("fake@broadinstitute.org", AgoraTestData.owner1.get, AgoraTestData.owner2.get)
  }
}

@DoNotDiscover
class AdminSweeperSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll with AgoraTestFixture with ImplicitSender {

  override protected def beforeAll() = {
  ensureDatabasesAreRunning()
  }

  override protected def afterAll() = {
  clearDatabases()
  }

  "Agora" should {
    "be able to synchronize it's list of admins via the AdminSweeper" in {
    addAdminUser()

    val adminSweeper = TestActorRef(AdminSweeper.props(AdminSweeperSpec.getMockAdminsList, permsDataSource))
    within(30 seconds) {
      adminSweeper ! Sweep
      awaitAssert{
        runInDB { db =>
          db.admPerms.listAdminUsers
        } == AdminSweeperSpec.getMockAdminsList
      }
    }
  }

  }

}
