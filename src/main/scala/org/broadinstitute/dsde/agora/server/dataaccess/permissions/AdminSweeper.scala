package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.{Actor, Props}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.webservice.util.GoogleApiUtils
import slick.dbio.DBIOAction

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object AdminSweeper {
  def props(pollFunction: () => List[String], permissionsDataSource: PermissionsDataSource): Props = {
    Props(new AdminSweeper(pollFunction, permissionsDataSource))
  }

  case object Sweep

  case object Swept

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
}

/**
 * Poll for an updated list of admins, and update our user table to reflect this list.
 * Intended to be run via a scheduler from the parent actor
 * TODO- Implement bulk transactions for better scalability. Currently runs a DB transaction for each user whose admin status needs changing.
 */
class AdminSweeper(pollAdmins: () => List[String], permissionsDataSource: PermissionsDataSource) extends Actor {

  import AdminSweeper.{Sweep, Swept}
  implicit val executionContext: ExecutionContext = context.dispatcher

  override def receive: Receive = waiting

  private def waiting: Receive = {
    case Sweep =>
      synchronizeAdmins andThen { case _ => self ! Swept }
      context.become(sweeping)
    case Swept => /* Unexpected... still ignore it. Maybe this actor restarted. */
  }

  private def sweeping: Receive = {
    case Sweep => /* ignore, we're sweeping already */
    case Swept => context.become(waiting)
  }

  def synchronizeAdmins: Future[List[Int]] = {
    // get expected and observed admins lists
    Future(pollAdmins()) flatMap synchronizeTrueAdmins
  }

  private def synchronizeTrueAdmins(trueAdmins: List[String]): Future[List[Int]] = {
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
}
