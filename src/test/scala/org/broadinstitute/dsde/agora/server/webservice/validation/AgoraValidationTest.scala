package org.broadinstitute.dsde.agora.server.webservice.validation

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.{DoNotDiscover, FlatSpec}

/**
 * Created by dshiga on 5/18/15.
 */
@DoNotDiscover
class AgoraValidationTest extends FlatSpec {

  "Agora" should "reject method with empty namespace" in {
    val entity = AgoraEntity(namespace = Option(""), name = Option("test"), payload = Option("task test {}"))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 1)
  }

  "Agora" should "reject method with empty name" in {
    val entity = AgoraEntity(namespace = Option("test"), name = None, payload = Option("task test {}"))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 1)
  }

  "Agora" should "note multiple validation errors at once" in {
    val entity = AgoraEntity(namespace = Option(""), name = Option(""), payload = Option("task test {}"))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 2)
  }

  "Agora" should "reject method with synopsis > 80 characters" in {
    val entity = AgoraEntity(
      namespace = Option("test"),
      name = Option("test"),
      synopsis = Option("012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
      payload = Option("task test {}"))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 1)
  }
}
