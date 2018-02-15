package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import akka.pattern._
import akka.testkit.TestKitBase
import akka.util.Timeout
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor._
import org.broadinstitute.dsde.workbench.util.health.{HealthMonitor, StatusCheckResponse, SubsystemStatus}
import org.broadinstitute.dsde.workbench.util.health.StatusJsonSupport.StatusCheckResponseFormat
import org.broadinstitute.dsde.workbench.util.health.Subsystems.{Database, Mongo}
import spray.http.StatusCodes
import spray.httpx.SprayJsonSupport._
import org.scalatest.{DoNotDiscover, FlatSpecLike}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}


@DoNotDiscover
class AgoraServiceUnhealthyStatusSpec extends ApiServiceSpec with TestKitBase with FlatSpecLike {
  implicit val unitTestActorSystem = ActorSystem("AgoraServiceUnhealthyStatusSpec")
  // this health monitor uses the same SQL check as our runtime mysql, which calls VERSION() in the db.
  // H2 does not support VERSION(), so we expect this health monitor to show as unhealthy.
  val dbStatus = new AgoraDBStatus(permsDataSource)
  val healthMonitor = unitTestActorSystem.actorOf(HealthMonitor.props(Set(Database, Mongo)) { () =>
    Map(Database -> dbStatus.mysqlStatus, Mongo -> dbStatus.mongoStatus)
  }, "health-monitor")
  val apiStatusService = new StatusService(permsDataSource, healthMonitor) with ActorRefFactoryContext

  override def beforeAll: Unit = {
    implicit val askTimeout:Timeout = Timeout(1.minute) // timeout for the ask to healthMonitor for GetCurrentStatus
    ensureDatabasesAreRunning()
    // tell the health monitor to perform a check, then wait until checks have returned
    healthMonitor ! CheckAll
    awaitCond(
      {Await.result(ask(healthMonitor, GetCurrentStatus).mapTo[StatusCheckResponse], Duration.Inf)
        .systems.values.collect { case UnknownStatus => true }.isEmpty}
      , 5.seconds, 200.milliseconds)
  }

  override def afterAll() = {
    clearDatabases()
  }


  // our test sql db is H2, which doesn't allow us to check for version
  it should "not actually be able to test the sql db" in {
    Get(s"/status") ~> apiStatusService.statusRoute ~>
      check {
        assertResult(StatusCodes.InternalServerError) { status }
        val statusResponse = responseAs[StatusCheckResponse] // will throw error and fail test if can't deserialize
        assert( !statusResponse.ok )
        assertResult(Set(Mongo,Database)) { statusResponse.systems.keySet }

        assert( statusResponse.systems(Mongo).ok )
        assert( statusResponse.systems(Mongo).messages.isEmpty )

        assert( !statusResponse.systems(Database).ok )
        assert( statusResponse.systems(Database).messages.nonEmpty )
        assert( statusResponse.systems(Database).messages.getOrElse(List()).head.nonEmpty )
        assert( statusResponse.systems(Database).messages.getOrElse(List()).head.contains("""Function "VERSION" not found""") )
      }
  }
}

@DoNotDiscover
class AgoraServiceHealthyStatusSpec extends ApiServiceSpec with TestKitBase with FlatSpecLike {
  implicit val unitTestActorSystem = ActorSystem("AgoraServiceHealthyStatusSpec")
  // this health monitor uses the unit-test-only UnitTestAgoraDBStatus, which should work in H2.
  // therefore, we expect this health monitor to show as healthy.
  val dbStatus = new UnitTestAgoraDBStatus(permsDataSource)
  val healthMonitor = unitTestActorSystem.actorOf(HealthMonitor.props(Set(Database, Mongo)) { () =>
    Map(Database -> dbStatus.h2Status, Mongo -> dbStatus.mongoStatus)
  }, "health-monitor")
  val apiStatusService = new StatusService(permsDataSource, healthMonitor) with ActorRefFactoryContext

  override def beforeAll: Unit = {
    implicit val askTimeout:Timeout = Timeout(1.minute) // timeout for the ask to healthMonitor for GetCurrentStatus
    ensureDatabasesAreRunning()
    // tell the health monitor to perform a check, then wait until checks have returned
    healthMonitor ! CheckAll
    awaitCond(
      {Await.result(ask(healthMonitor, GetCurrentStatus).mapTo[StatusCheckResponse], Duration.Inf)
          .systems.values.collect { case UnknownStatus => true }.isEmpty}
      , 5.seconds, 200.milliseconds)
  }

  override def afterAll: Unit = {
    clearDatabases()
  }

  it should "run and connect to DBs" in {
    Get(s"/status") ~> apiStatusService.statusRoute ~>
      check {
        assertResult(StatusCodes.OK) { status }
        val statusResponse = responseAs[StatusCheckResponse] // will throw error and fail test if can't deserialize
        assert( statusResponse.ok )
        assertResult(Set(Mongo,Database)) { statusResponse.systems.keySet }

        assert( statusResponse.systems(Mongo).ok )
        assert( statusResponse.systems(Mongo).messages.isEmpty )

        assert( statusResponse.systems(Database).ok )
        assert( statusResponse.systems(Database).messages.isEmpty )
      }
  }


  class UnitTestAgoraDBStatus(dataSource: PermissionsDataSource)(implicit ec: ExecutionContext)
    extends AgoraDBStatus(dataSource: PermissionsDataSource) {

    // since H2 doesn't support the version function, we call an arbitrary (and less performant) SQL query
    // in order to unit-test a good status response.
    def h2Status: Future[SubsystemStatus] = {
      dataSource.inTransaction { db =>
        db.admPerms.listAdminUsers.asTry map {
          case Success(s) => HealthMonitor.OkStatus
          case Failure(t) => HealthMonitor.failedStatus(t.getMessage)
        }
      }
    }

  }
}
