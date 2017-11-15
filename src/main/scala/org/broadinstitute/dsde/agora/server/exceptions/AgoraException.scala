package org.broadinstitute.dsde.agora.server.exceptions

import com.typesafe.scalalogging.LazyLogging
import spray.http.{StatusCode, StatusCodes}

case class AgoraException(message: String = null,
                          cause: Throwable = null,
                          statusCode: StatusCode = StatusCodes.InternalServerError)
  extends Exception(message, cause) with LazyLogging {
  override def getMessage: String = {
    s"Cause: $cause" + " Message: $message"
  }
}

object AgoraException {
  def apply(message: String, statusCode: StatusCode): AgoraException =
    new AgoraException(message, null, statusCode)
  def apply(message: String, cause: Option[Throwable], statusCode: StatusCode): AgoraException =
    new AgoraException(message, cause.orNull, statusCode)
}