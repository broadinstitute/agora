
package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._

object AgoraPermissions {
  val Nothing: Int = 0
  val Read: Int = 1 << 0
  val Write: Int = 1 << 1
  val Create: Int = 1 << 2
  val Redact: Int = 1 << 3
  val Manage: Int = 1 << 4
  val ReadWrite: Int = Read | Write
  val All: Int = Read | Write | Create | Redact | Manage

  implicit class EnhancedAgoraPermissions(val givenPermission: Int) extends AnyVal {
    /**
     * Returns true if this permission has the passed checkPermission.
     */
    def hasPermission(checkPermission: Int): Boolean = {
      (givenPermission & checkPermission) == checkPermission
    }
  }

  // The roles String input is formatted like "read, write, manage"
  def fromParams(roles: String): AgoraPermissions = {
    AgoraPermissions(roles.split(",").toIndexedSeq)
  }

  def apply(roles: Seq[String]): AgoraPermissions = {
    var permissionNumber = 0
    val lowerCaseRoles = roles.map(_.toLowerCase)
    if (lowerCaseRoles.contains("read")) permissionNumber += Read
    if (lowerCaseRoles.contains("write")) permissionNumber += Write
    if (lowerCaseRoles.contains("create")) permissionNumber += Create
    if (lowerCaseRoles.contains("redact")) permissionNumber += Redact
    if (lowerCaseRoles.contains("manage")) permissionNumber += Manage

    // All takes precedence over other permissions
    if (lowerCaseRoles.contains("all")) permissionNumber = All

    // Nothing takes precedences over All 
    if (lowerCaseRoles.contains("nothing")) permissionNumber = Nothing

    AgoraPermissions(permissionNumber)
  }
}

case class AgoraPermissions(permissions: Int) {
  def this(varPermissions: Int*) = {
    this(varPermissions.foldLeft(0) { (perm1, perm2) => perm1 | perm2 })
  }

  def removePermissions(varPermissions: Int*): AgoraPermissions = {
    AgoraPermissions(varPermissions.foldLeft(permissions) { (perm1, perm2) => perm1 & ~perm2 })
  }

  def addPermissions(varPermissions: Int*): AgoraPermissions = {
    AgoraPermissions(varPermissions.foldLeft(permissions) { (perm1, perm2) => perm1 | perm2 })
  }

  def hasPermission(perm: AgoraPermissions): Boolean = (permissions & perm.permissions) != 0

  def canRead: Boolean = (permissions & Read) != 0

  def canWrite: Boolean = (permissions & Write) != 0

  def canCreate: Boolean = (permissions & Create) != 0

  def canRedact: Boolean = (permissions & Redact) != 0

  def canManage: Boolean = (permissions & Manage) != 0

  def toInt: Int = permissions

  def toListOfStrings: Vector[String] = {
    var permissionList: Vector[String] = Vector()
    if (this.canRead) permissionList = permissionList :+ "Read"
    if (this.canWrite) permissionList = permissionList :+ "Write"
    if (this.canCreate) permissionList = permissionList :+ "Create"
    if (this.canRedact) permissionList = permissionList :+ "Redact"
    if (this.canManage) permissionList = permissionList :+ "Manage"

    permissionList
  }

  override def toString: String = {
    this.toListOfStrings.mkString(", ")
  }

}
