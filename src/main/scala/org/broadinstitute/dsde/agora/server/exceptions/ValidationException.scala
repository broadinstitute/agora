package org.broadinstitute.dsde.agora.server.exceptions

import com.typesafe.scalalogging.LazyLogging

case class ValidationException(message: String, ex: Throwable = null) extends Exception with LazyLogging {
  override def getMessage: String = message
}
