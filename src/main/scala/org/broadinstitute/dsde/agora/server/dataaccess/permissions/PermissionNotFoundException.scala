package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import com.typesafe.scalalogging.slf4j.LazyLogging

class PermissionNotFoundException(message: String = "Could not find permissions.", ex: Throwable) extends Exception with LazyLogging {
  logger.error(ex.getMessage)
  override def getMessage: String = message
}
