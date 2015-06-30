package org.broadinstitute.dsde.agora.server.dataaccess.acls

import spray.http.StatusCode

case class ClientServiceFailure(statusCode: StatusCode, message: String) extends Throwable
