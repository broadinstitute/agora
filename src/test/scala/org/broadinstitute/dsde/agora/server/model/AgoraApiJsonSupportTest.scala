package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport.AgoraValidationFormat
import org.broadinstitute.dsde.agora.server.webservice.validation.AgoraValidation
import org.scalatest.{DoNotDiscover, FlatSpec}
import spray.json._

/**
 * Created by dshiga on 5/18/15.
 */
@DoNotDiscover
class AgoraApiJsonSupportTest extends FlatSpec with DefaultJsonProtocol {

  "Agora" should "convert AgoraValidation to JSON" in {
    val validation = AgoraValidation(Seq("test1", "test2"))
    val json = AgoraValidationFormat.write(validation)
    val error = json.asJsObject.fields("error")
    assert(error.toString.contains("test1") === true)
    assert(error.toString.contains("test2") === true)
  }

  "Agora" should "convert JSON to AgoraValidation" in {
    val json = Map("error" -> Seq("test1", "test2")).toJson
    val validation = AgoraValidationFormat.read(json)
    assert(validation.messages.contains("test1") === true)
    assert(validation.messages.contains("test2") === true)
  }
}
