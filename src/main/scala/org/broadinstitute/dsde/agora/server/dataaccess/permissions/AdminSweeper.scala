package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.{Actor, Props}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.webservice.util.GoogleApiUtils
import slick.dbio.DBIOAction

import scala.collection.JavaConverters._
import scala.concurrent.Future

object AdminSweeper {
  def props(pollFunction: () => List[String], permissionsDataSource: PermissionsDataSource): Props = Props(classOf[AdminSweeper], pollFunction, permissionsDataSource)

  case class Sweep()

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
  import AdminSweeper.Sweep
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def receive = {
    case Sweep => synchronizeAdmins
  }
  def synchronizeAdmins: Future[List[Int]] = {
    // get expected and observed admins lists
    val trueAdmins: List[String] = pollAdmins()

    permissionsDataSource.inTransaction { db =>
      db.admPerms.listAdminUsers flatMap { currentAdmins =>
        // Difference the lists
        val newAdmins = trueAdmins.filterNot(currentAdmins.toSet)
        val adminsToDelete = currentAdmins.filterNot(trueAdmins.toSet)

        // Update our user table to reflect list differences
        val updateActions = newAdmins map { newAdmin =>
          db.admPerms.updateAdmin(newAdmin, true)
        }
        val deleteActions = adminsToDelete map { adminToDelete =>
          db.admPerms.updateAdmin(adminToDelete, false)
        }

        DBIOAction.sequence(updateActions ++ deleteActions)
      }
    }
  }
}
