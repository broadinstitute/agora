package org.broadinstitute.dsde.agora.server.actor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDataCache
import org.broadinstitute.dsde.agora.server.dataaccess.health.{HealthMonitorSubsystems, TypedHealthMonitor}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AdminSweeper, PermissionsDataSource}
import org.broadinstitute.dsde.workbench.util.health.StatusCheckResponse

import scala.concurrent.duration._

/**
 * Top level typed guardian actor for the Agora actor system.
 *
 * Run the health monitor and optionally the admin sweeper.
 */
object AgoraGuardianActor {

  //@formatter:off
  sealed trait Command
  case class GetCurrentStatus(replyTo: ActorRef[StatusCheckResponse]) extends Command
  case class GetDataCache(replyTo: ActorRef[AgoraDataCache]) extends Command
  case class FlushDataCache(replyTo: ActorRef[ReadCacheActor.Flushed.type]) extends Command
  //@formatter:on

  def apply(permissionsDataSource: PermissionsDataSource,
            subsystems: HealthMonitorSubsystems,
            ioDispatcherSelector: DispatcherSelector): Behavior[Command] = {
    Behaviors.setup { context =>
      val log = context.log
      val ioDispatcher = context.system.dispatchers.lookup(ioDispatcherSelector)

      val dataCacheActorRefOption: Option[ActorRef[ReadCacheActor.Command]] = AgoraConfig.dataCacheDuration match {
        case None =>
          log.info("Data cache not enabled.")
          None
        case Some(dataCacheDurationActual) =>
          val dataCacheDispatcherPath = "dispatchers.data-cache-dispatcher"
          log.info(s"Data cache refreshing every $dataCacheDurationActual.")
          val ref = context.spawn(
            new ReadCacheActor[AgoraDataCache].create(ReadCacheActor.Params(
              "dataCache",
              dataCacheDurationActual,
              () => AgoraBusiness.getAgoraDataCache(permissionsDataSource)(ioDispatcher)
            )),
            "data-cache",
            DispatcherSelector.fromConfig(dataCacheDispatcherPath)
          )
          Option(ref)
      }

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
          log.info(s"Admin sweeper running every ${AgoraConfig.adminSweepInterval.seconds}.")
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
        case GetDataCache(replyTo) =>
          dataCacheActorRefOption.foreach(_ ! ReadCacheActor.Request(replyTo))
          Behaviors.same
        case FlushDataCache(replyTo) =>
          dataCacheActorRefOption.foreach(_ ! ReadCacheActor.Flush(replyTo))
          Behaviors.same
      }
    }
  }
}
