package org.broadinstitute.dsde.agora.server.exceptions

import com.typesafe.scalalogging.slf4j.LazyLogging
import spray.http.{StatusCode, StatusCodes}

case class AgoraException(message: String = null,
                          cause: Throwable = null,
                          statusCode: StatusCode = StatusCodes.InternalServerError)
  extends Exception(message, cause) with LazyLogging {
  override def getMessage: String = {
    s"Cause: $cause" + " Message: $message"
  }
}