package org.broadinstitute.dsde.agora.server.exceptions

import com.typesafe.scalalogging.slf4j.LazyLogging

case class PermissionNotFoundException(message: String = "Could not find permissions.", ex: Throwable = null) extends Exception(message, ex) with LazyLogging {
  //override def getMessage: String = message
}
