package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.{AgoraAkkaTest, AgoraTestData, AgoraTestFixture}
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, Matchers, WordSpecLike}

import scala.concurrent.duration._

object AdminSweeperSpec {
  def getMockAdminsList:() =>  List[String] = { () =>
    List("fake@broadinstitute.org", AgoraTestData.owner1.get, AgoraTestData.owner2.get)
  }
}

@DoNotDiscover
class AdminSweeperSpec
  extends WordSpecLike with Matchers with BeforeAndAfterAll with AgoraTestFixture with AgoraAkkaTest {

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  ensureDatabasesAreRunning()
  }

  override protected def afterAll(): Unit = {
  clearDatabases()
    super.afterAll()
  }

  "Agora" should {
    "be able to synchronize it's list of admins via the AdminSweeper" in {
      addAdminUser()

      actorTestKit.spawn(AdminSweeper(AdminSweeperSpec.getMockAdminsList, permsDataSource, 5.seconds, 5.seconds))
      val testProbe = actorTestKit.createTestProbe("within-and-assert")
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
