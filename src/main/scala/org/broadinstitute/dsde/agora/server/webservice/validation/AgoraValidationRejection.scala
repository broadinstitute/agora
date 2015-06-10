package org.broadinstitute.dsde.agora.server.webservice.validation

import spray.routing.Rejection

case class AgoraValidationRejection(validation: Seq[AgoraValidation]) extends Rejection
