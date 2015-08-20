package org.broadinstitute.dsde.agora.server.exceptions

import com.typesafe.scalalogging.slf4j.LazyLogging
import spray.http.{StatusCode, StatusCodes}

case class AgoraException(message: String = null,
                          cause: Throwable = null,
                          statusCode: StatusCode = StatusCodes.InternalServerError)
  extends Exception(message, cause) with LazyLogging {
  logger.error("-"*20)
  logger.error(s"Cause: $cause")
  logger.error(s"Message: $message")
  logger.error("-"*20)
}