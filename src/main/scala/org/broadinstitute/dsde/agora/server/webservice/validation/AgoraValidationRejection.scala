package org.broadinstitute.dsde.agora.server.webservice.validation

import spray.routing.{Rejection, ValidationRejection}

/**
 * Created by dshiga on 5/18/15.
 */
case class AgoraValidationRejection(validation: AgoraValidation) extends Rejection
