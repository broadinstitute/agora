package org.broadinstitute.dsde.agora.server.dataaccess.health

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter._
import org.broadinstitute.dsde.workbench.util.health.{HealthMonitor, StatusCheckResponse}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Akka Typed wrapper around the Workbench Health Monitor.
 * Runs its own schedule sending messages to itself, instead of requiring others to send in messages.
 */
object TypedHealthMonitor {

  //@formatter:off
  sealed trait Command
  private case object Check extends Command
  case class GetCurrentStatus(replyTo: ActorRef[StatusCheckResponse]) extends Command

  private case object CheckSingleTimer
  private case object CheckRateTimer
  //@formatter:on

  def apply(healthMonitorSubsystems: HealthMonitorSubsystems,
            initialDelay: FiniteDuration,
            fixedRate: FiniteDuration,
            dispatcherName: String): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        implicit val executionContext: ExecutionContext = context.executionContext
        val healthMonitor = context.actorOf(
          HealthMonitor
            .props(healthMonitorSubsystems.subsystems)(healthMonitorSubsystems.checkHealth)
            .withDispatcher(dispatcherName)
        )

        timers.startSingleTimer(CheckSingleTimer, Check, initialDelay)
        timers.startTimerAtFixedRate(CheckRateTimer, Check, fixedRate)

        Behaviors.receiveMessage {
          case Check =>
            healthMonitor ! HealthMonitor.CheckAll
            Behaviors.same
          case GetCurrentStatus(replyTo) =>
            healthMonitor.tell(HealthMonitor.GetCurrentStatus, replyTo.toClassic)
            Behaviors.same
        }
      }
    }
  }
}
