package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.webservice.util.GoogleApiUtils
import org.slf4j.Logger
import slick.dbio.DBIOAction

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object AdminSweeper {

  //@formatter:off
  sealed trait Command
  private case object Sweep extends Command
  private case object Swept extends Command

  private case object SweepSingleTimer
  private case object SweepRateTimer
  //@formatter:on

  /**
   * Function to poll for the member-emails of a config-defined google group.
   * Google group is assumed to contain an up-to-date list of admins as members.
   */
  val adminsGoogleGroupPoller: () => List[String] = { () =>
    GoogleApiUtils.getGroupDirectory
      .members
      .list(AgoraConfig.adminGoogleGroup.get)
      .execute
      .getMembers
      .asScala
      .map(_.getEmail)
      .toList
  }

  def apply(pollAdmins: () => List[String],
            permissionsDataSource: PermissionsDataSource,
            initialDelay: FiniteDuration,
            fixedRate: FiniteDuration): Behavior[Command] = {
    Behaviors.setup { _ =>
      Behaviors.withTimers { timers =>
        timers.startSingleTimer(SweepSingleTimer, Sweep, initialDelay)
        timers.startTimerAtFixedRate(SweepRateTimer, Sweep, fixedRate)
        run(pollAdmins, permissionsDataSource)
      }
    }
  }

  /**
   * Poll for an updated list of admins, and update our user table to reflect this list.
   * Intended to be run via a scheduler from the parent actor
   * TODO- Implement bulk transactions for better scalability. Currently runs a DB transaction for each user whose admin status needs changing.
   */
  private def run(pollAdmins: () => List[String], permissionsDataSource: PermissionsDataSource): Behavior[Command] = {

    def waiting(): Behavior[Command] = {
      Behaviors.receive { (context, message) =>
        message match {
          case Sweep =>
            val log: Logger = context.log
            context.pipeToSelf(synchronizeAdmins(log)(context.executionContext)) {
              case Failure(NonFatal(nonFatal)) =>
                log.error(s"Error synchronizing admins: ${nonFatal.getMessage}", nonFatal)
                Swept
              case Failure(throwable) =>
                log.error(s"Fatal error synchronizing admins: ${throwable.getMessage}", throwable)
                throw throwable
              case Success(_) => Swept
            }
            sweeping()
          case Swept =>
            Behaviors.same
        }
      }
    }

    def sweeping(): Behavior[Command] = {
      Behaviors.receiveMessage {
        case Sweep =>
          Behaviors.same
        case Swept =>
          waiting()
      }
    }

    def synchronizeAdmins(log: Logger)(implicit executionContext: ExecutionContext): Future[List[Int]] = {
      // get expected and observed admins lists
      for {
        _ <- Future.successful(log.info(s"Beginning poll for admins"))
        polled <- Future(pollAdmins())
        _ <- Future.successful(log.info(s"Retrieved admins: ${polled.size}"))
        synchronized <- synchronizeTrueAdmins(polled)
        _ <- Future.successful(log.info(s"Synchronization adjusted admins: ${synchronized.sum}"))
      } yield synchronized
    }

    def synchronizeTrueAdmins(trueAdmins: List[String])
                             (implicit executionContext: ExecutionContext): Future[List[Int]] = {
      permissionsDataSource.inTransaction { db =>
        db.admPerms.listAdminUsers() flatMap { currentAdmins =>
          // Difference the lists
          val newAdmins = trueAdmins.filterNot(currentAdmins.toSet)
          val adminsToDelete = currentAdmins.filterNot(trueAdmins.toSet)

          // Update our user table to reflect list differences
          val updateActions = newAdmins map { newAdmin =>
            db.admPerms.updateAdmin(newAdmin, adminStatus = true)
          }
          val deleteActions = adminsToDelete map { adminToDelete =>
            db.admPerms.updateAdmin(adminToDelete, adminStatus = false)
          }

          DBIOAction.sequence(updateActions ++ deleteActions)
        }
      }
    }

    waiting()
  }
}
