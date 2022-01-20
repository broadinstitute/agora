package org.broadinstitute.dsde.agora.server.exceptions

case class PermissionModificationException() extends Exception {
  override def getMessage: String = { "You may not change your own permissions" }
}
