package org.broadinstitute.dsde.agora.server.acls

/**
 * Created by dshiga on 6/16/15.
 */

object GcsRole {
  val Reader = "READER"
  val Writer = "WRITER"
  val Owner = "OWNER"
}

case class GcsRole(role: String) {
  
  def isReader: Boolean = role.equals(GcsRole.Reader) || role.equals(GcsRole.Writer) || role.equals(GcsRole.Owner)
  
  def isWriter: Boolean = role.equals(GcsRole.Writer) || role.equals(GcsRole.Owner)
  
  def isOwner: Boolean = role.equals(GcsRole.Owner)
}
