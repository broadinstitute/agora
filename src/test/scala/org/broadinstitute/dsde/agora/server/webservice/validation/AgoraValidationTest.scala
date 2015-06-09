package org.broadinstitute.dsde.agora.server.webservice.validation

import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraProjectionDefaults, AgoraEntity}
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class AgoraValidationTest extends FlatSpec {

  "Agora" should "reject method with empty namespace" in {
    val entity = AgoraEntity(namespace = Option(""), name = Option("test"), payload = Option("task test {}"), entityType = Option(AgoraEntityType.Task))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 1)
  }

  "Agora" should "reject method with empty name" in {
    val entity = AgoraEntity(namespace = Option("test"), name = None, payload = Option("task test {}"), entityType = Option(AgoraEntityType.Task))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 1)
  }

  "Agora" should "note multiple method validation errors at once" in {
    val entity = AgoraEntity(namespace = Option(""), name = Option(""), payload = Option("task test {}"), entityType = Option(AgoraEntityType.Task))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 2)
  }

  "Agora" should "reject method with synopsis > 80 characters" in {
    val entity = AgoraEntity(
      namespace = Option("test"),
      name = Option("test"),
      synopsis = Option("012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
      payload = Option("task test {}"),
      entityType = Option(AgoraEntityType.Task))
    val validation = AgoraValidation.validateMetadata(entity)
    assert(validation.valid === false)
    assert(validation.messages.size === 1)
  }

  "Agora" should "reject projection with both exclude and include fields" in {
    val validation = AgoraValidation.validateIncludeExcludeFields(Seq("foo"), Seq("bar"))
    assert(validation.valid === false)
    assert(validation.messages.size === 1)
  }
  
  "Agora" should "reject projection that excludes required fields" in {
    val validation = AgoraValidation.validateIncludeExcludeFields(Seq.empty[String], AgoraProjectionDefaults.RequiredProjectionFields)
    assert(validation.valid === false)
    assert(validation.messages.size === 3)
  }

  "Agora" should "accept projection that doesn't violate any validation rules" in {
    val validation = AgoraValidation.validateIncludeExcludeFields(Seq("foo"), Seq.empty[String])
    assert(validation.valid === true)
  }


}
