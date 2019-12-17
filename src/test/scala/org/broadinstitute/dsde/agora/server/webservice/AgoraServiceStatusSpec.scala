package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.RouteTest
import akka.util.Timeout
import org.broadinstitute.dsde.agora.server.actor
import org.broadinstitute.dsde.agora.server.actor.AgoraGuardianActor
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.agora.server.dataaccess.health.AgoraHealthMonitorSubsystems._
import org.broadinstitute.dsde.agora.server.dataaccess.health.HealthMonitorSubsystems
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor._
import org.broadinstitute.dsde.workbench.util.health.StatusJsonSupport.StatusCheckResponseFormat
import org.broadinstitute.dsde.workbench.util.health.Subsystems.{Database, Mongo}
import org.broadinstitute.dsde.workbench.util.health.{HealthMonitor, StatusCheckResponse, SubsystemStatus}
import org.scalatest.{DoNotDiscover, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}


@DoNotDiscover
class AgoraServiceUnhealthyStatusSpec extends ApiServiceSpec with Matchers with FlatSpecLike with RouteTest {
  // this health monitor uses the same SQL check as our runtime mysql, which calls VERSION() in the db.
  // H2 does not support VERSION(), so we expect this health monitor to show as unhealthy.
  private lazy val dbStatus = new AgoraDBStatus(permsDataSource)
  override lazy val agoraGuardian: ActorRef[AgoraGuardianActor.Command] = actorTestKit.spawn(actor.AgoraGuardianActor(
    permsDataSource,
    dbStatus.toHealthMonitorSubsystems,
    DispatcherSelector.sameAsParent()
  ))
  private lazy val apiStatusService = new StatusService(permsDataSource, agoraGuardian)

  override def beforeAll: Unit = {
    super.beforeAll()
    implicit val askTimeout:Timeout = Timeout(1.minute) // timeout for the ask to healthMonitor for GetCurrentStatus
    ensureDatabasesAreRunning()
    // tell the health monitor to perform a check, then wait until checks have returned
    val testProbe = actorTestKit.createTestProbe()
    testProbe.awaitAssert(
      {
        val result = Await.result(agoraGuardian.ask(AgoraGuardianActor.GetCurrentStatus), askTimeout.duration)
        val filtered = result.systems.values.collect { case UnknownStatus => true }
        filtered should be(empty)
      },
      5.seconds,
      200.milliseconds
    )
  }

  override def afterAll(): Unit = {
    clearDatabases()
    super.afterAll()
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
class AgoraServiceHealthyStatusSpec extends ApiServiceSpec with Matchers with FlatSpecLike {
  // this health monitor uses the unit-test-only UnitTestAgoraDBStatus, which should work in H2.
  // therefore, we expect this health monitor to show as healthy.
  private lazy val dbStatus = new UnitTestAgoraDBStatus(permsDataSource)
  override lazy val agoraGuardian: ActorRef[AgoraGuardianActor.Command] = actorTestKit.spawn(actor.AgoraGuardianActor(
    permsDataSource,
    new HealthMonitorSubsystems(Map(
      Database -> (_ => dbStatus.h2Status),
      Mongo -> (executionContext => dbStatus.mongoStatus()(executionContext))
    )),
    DispatcherSelector.sameAsParent()
  ))
  private lazy val apiStatusService = new StatusService(permsDataSource, agoraGuardian)

  override def beforeAll: Unit = {
    super.beforeAll()
    implicit val askTimeout:Timeout = Timeout(1.minute) // timeout for the ask to healthMonitor for GetCurrentStatus
    ensureDatabasesAreRunning()
    // tell the health monitor to perform a check, then wait until checks have returned
    val testProbe = actorTestKit.createTestProbe()
    testProbe.awaitAssert(
      {
        val result = Await.result(agoraGuardian.ask(AgoraGuardianActor.GetCurrentStatus), askTimeout.duration)
        val filtered = result.systems.values.collect { case UnknownStatus => true }
        filtered should be(empty)
      },
      5.seconds,
      200.milliseconds
    )
  }

  override def afterAll: Unit = {
    clearDatabases()
    super.afterAll()
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


  class UnitTestAgoraDBStatus(dataSource: PermissionsDataSource)
    extends AgoraDBStatus(dataSource: PermissionsDataSource) {

    // since H2 doesn't support the version function, we call an arbitrary (and less performant) SQL query
    // in order to unit-test a good status response.
    def h2Status: Future[SubsystemStatus] = {
      dataSource.inTransaction { db =>
        db.admPerms.listAdminUsers.asTry map {
          case Success(_) => HealthMonitor.OkStatus
          case Failure(t) => HealthMonitor.failedStatus(t.getMessage)
        }
      }
    }

  }
}
