package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.testkit.typed.scaladsl._
import org.broadinstitute.dsde.agora.server.{AgoraTestData, AgoraTestFixture}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object AdminSweeperSpec {
  def getMockAdminsList:() =>  List[String] = { () =>
    List("fake@broadinstitute.org", AgoraTestData.owner1.get, AgoraTestData.owner2.get)
  }
}

@DoNotDiscover
class AdminSweeperSpec extends AnyWordSpecLike with Matchers with BeforeAndAfterAll with AgoraTestFixture {

  private implicit val testKit: ActorTestKit = ActorTestKit("AdminSweeperSpec")
  private implicit val executionContext: ExecutionContext = testKit.system.executionContext

  override protected def beforeAll(): Unit = {
  ensureDatabasesAreRunning()
  }

  override protected def afterAll(): Unit = {
  clearDatabases()
    testKit.shutdownTestKit()
  }

  "Agora" should {
    "be able to synchronize it's list of admins via the AdminSweeper" in {
      addAdminUser()

      testKit.spawn(AdminSweeper(AdminSweeperSpec.getMockAdminsList, permsDataSource, 5.seconds, 5.seconds))
      val testProbe = testKit.createTestProbe("within-and-assert")
      testProbe.within(30.seconds) {
        testProbe.awaitAssert {
          runInDB { db =>
            db.admPerms.listAdminUsers()
          } == AdminSweeperSpec.getMockAdminsList
        }
      }
    }

  }

}
