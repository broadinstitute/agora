package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import akka.actor.{Actor, Props}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.webservice.util.GoogleApiUtils
import com.google.api.services.admin.directory.model.Member
import scala.collection.JavaConversions._

object AdminSweeper {
  def props(pollFunction: () => List[String]): Props = Props(classOf[AdminSweeper], pollFunction)

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
      .toList
      .map(_.getEmail)
  }
}

/**
 * Poll for an updated list of admins, and update our user table to reflect this list.
 * Intended to be run via a scheduler from the parent actor
 * TODO- Implement bulk transactions for better scalability. Currently runs a DB transaction for each user whose admin status needs changing.
 */
class AdminSweeper(pollAdmins: () => List[String]) extends Actor {
  import AdminSweeper.Sweep
  def receive = {
    case Sweep => synchronizeAdmins
  }
  def synchronizeAdmins: Unit = {
    // get expected and observed admins lists
    val trueAdmins: List[String] = pollAdmins()
    val currentAdmins = AdminPermissionsClient.listAdminUsers

    // Difference the lists
    val newAdmins = trueAdmins.filterNot(currentAdmins.toSet)
    val adminsToDelete = currentAdmins.filterNot(trueAdmins.toSet)

    // Update our user table to reflect list differences
    for (newAdmin <- newAdmins) {
      println(newAdmin)
      AdminPermissionsClient.updateAdmin(newAdmin, true)
    }

    for (adminToDelete <- adminsToDelete) {
      AdminPermissionsClient.updateAdmin(adminToDelete, false)
    }
  }


}
