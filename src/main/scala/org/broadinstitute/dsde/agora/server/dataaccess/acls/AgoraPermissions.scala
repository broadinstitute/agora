
package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions._

object AgoraPermissions {
  val Nothing = 0
  val Read = 1 << 0
  val Write = 1 << 1
  val Create = 1 << 2
  val Redact = 1 << 3
  val Manage = 1 << 4
  val ReadWrite = Read | Write
  val All = Read | Write | Create | Redact | Manage
}

case class AgoraPermissions(permissions: Int) {
  def this(varPermissions: Int*) {
    this(varPermissions.foldLeft(0) { (perm1, perm2) => perm1 | perm2 })
  }

  def removePermissions(varPermissions: Int*) = {
    AgoraPermissions(varPermissions.foldLeft(permissions) { (perm1, perm2) => perm1 & ~perm2 })
  }

  def addPermissions(varPermissions: Int*) = {
    AgoraPermissions(varPermissions.foldLeft(permissions) { (perm1, perm2) => perm1 | perm2 })
  }

  def canRead: Boolean = (permissions & Read) != 0

  def canWrite: Boolean = (permissions & Write) != 0

  def canCreate: Boolean = (permissions & Create) != 0

  def canRedact: Boolean = (permissions & Redact) != 0

  def canManage: Boolean = (permissions & Manage) != 0

}
