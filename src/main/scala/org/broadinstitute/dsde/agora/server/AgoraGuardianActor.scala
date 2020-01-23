package org.broadinstitute.dsde.agora.server

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.broadinstitute.dsde.agora.server.dataaccess.health.{HealthMonitorSubsystems, TypedHealthMonitor}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AdminSweeper, PermissionsDataSource}
import org.broadinstitute.dsde.workbench.util.health.StatusCheckResponse

import scala.concurrent.duration._

/**
 * Top level typed guardian actor for the Agora actor system.
 *
 * Run the health monitor and optionally the admin sweeper.
 *
 * For more info on the single guardian actor in Akka Typed see the Akka documentation, including:
 * https://doc.akka.io/docs/akka/current/typed/from-classic.html#actorsystem
 */
object AgoraGuardianActor {

  //@formatter:off
  sealed trait Command
  case class GetCurrentStatus(replyTo: ActorRef[StatusCheckResponse]) extends Command
  //@formatter:on

  def apply(permissionsDataSource: PermissionsDataSource, subsystems: HealthMonitorSubsystems): Behavior[Command] = {
    Behaviors.setup { context =>
      val log = context.log

      val healthMonitorDispatcherPath = "dispatchers.health-monitor-dispatcher"
      val healthMonitorActorRef = context.spawn(
        TypedHealthMonitor(subsystems, 3.seconds, 1.minute, healthMonitorDispatcherPath),
        "health-monitor",
        DispatcherSelector.fromConfig(healthMonitorDispatcherPath)
      )

      AgoraConfig.adminGoogleGroup match {
        case None => log.info("Admin sweeper not enabled.")
        case Some(_) =>
          val adminSweeperDispatcherPath = "dispatchers.admin-sweeper-dispatcher"
          log.info("Admin sweeper spawning.")
          context.spawn(
            AdminSweeper(
              AdminSweeper.adminsGoogleGroupPoller,
              permissionsDataSource,
              5.seconds,
              AgoraConfig.adminSweepInterval.seconds
            ),
            "admin-sweeper",
            DispatcherSelector.fromConfig(adminSweeperDispatcherPath)
          )
      }

      Behaviors.receiveMessage {
        case GetCurrentStatus(replyTo) =>
          healthMonitorActorRef ! TypedHealthMonitor.GetCurrentStatus(replyTo)
          Behaviors.same
      }
    }
  }
}
