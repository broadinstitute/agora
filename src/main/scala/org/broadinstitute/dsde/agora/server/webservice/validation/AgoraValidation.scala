package org.broadinstitute.dsde.agora.server.webservice.validation

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

/**
 * Created by dshiga on 5/18/15.
 */

object AgoraValidation {
  def validateMetadata(entity: AgoraEntity): AgoraValidation = {
    var validation = AgoraValidation()
    if (!entity.namespace.exists(_.trim.nonEmpty)) {
      validation = validation.addError("Namespace is required")
    }
    if (!entity.name.exists(_.trim.nonEmpty)) {
      validation = validation.addError("Name is required")
    }
    if (!entity.synopsis.forall(_.length() <= 80)) {
      validation = validation.addError("Synopsis must be 80 characters or less")
    }
    validation
  }
}

case class AgoraValidation(messages: Seq[String] = Seq.empty[String]) {

  def addError(message: String):AgoraValidation = {
    copy(messages = messages :+ message)
  }

  def valid = messages.size == 0
}
