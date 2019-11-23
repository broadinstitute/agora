package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper.Sweep
import org.broadinstitute.dsde.agora.server.{AgoraTestData, AgoraTestFixture}
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, Matchers, WordSpecLike}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

object AdminSweeperSpec {
  def getMockAdminsList:() =>  List[String] = { () =>
    List("fake@broadinstitute.org", AgoraTestData.owner1.get, AgoraTestData.owner2.get)
  }
}

@DoNotDiscover
class AdminSweeperSpec(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with Matchers with BeforeAndAfterAll with AgoraTestFixture with ImplicitSender {

  implicit val executionContext: ExecutionContext = _system.dispatcher

  override protected def beforeAll(): Unit = {
  ensureDatabasesAreRunning()
  }

  override protected def afterAll(): Unit = {
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
          db.admPerms.listAdminUsers()
        } == AdminSweeperSpec.getMockAdminsList
      }
    }
  }

  }

}
