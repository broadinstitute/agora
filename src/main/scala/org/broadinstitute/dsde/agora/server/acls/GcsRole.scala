package org.broadinstitute.dsde.agora.server.acls

import org.broadinstitute.dsde.agora.server.acls.GcsRole._

object GcsRole {
  val Reader = "READER"
  val Writer = "WRITER"
  val Owner = "OWNER"
  val Nothing = "NOTHING"
}

case class GcsBucketRole(role: String) {

  def isReader: Boolean = role.equals(Reader) || role.equals(Writer) || role.equals(Owner)

  def isWriter: Boolean = role.equals(Writer) || role.equals(Owner)

  def isOwner: Boolean = role.equals(Owner)
}

case class GcsObjectRole(role: String) {

  def isReader: Boolean = role.equals(Reader) || role.equals(Owner)

  def isOwner: Boolean = role.equals(Owner)
}
