
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpEntity}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.ValidationRejection

@DoNotDiscover
class AgoraMethodsSpec extends ApiServiceSpec {

  var testEntity1WithId: AgoraEntity = _
  var testEntity2WithId: AgoraEntity = _
  var testEntity3WithId: AgoraEntity = _
  var testEntity4WithId: AgoraEntity = _
  var testEntity5WithId: AgoraEntity = _
  var testEntity6WithId: AgoraEntity = _
  var testEntity7WithId: AgoraEntity = _
  var testEntityToBeRedactedWithId: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    testEntity1WithId = agoraBusiness.insert(testEntity1, mockAutheticatedOwner.get)
    testEntity2WithId = agoraBusiness.insert(testEntity2, mockAutheticatedOwner.get)
    testEntity3WithId = agoraBusiness.insert(testEntity3, mockAutheticatedOwner.get)
    testEntity4WithId = agoraBusiness.insert(testEntity4, mockAutheticatedOwner.get)
    testEntity5WithId = agoraBusiness.insert(testEntity5, mockAutheticatedOwner.get)
    testEntity6WithId = agoraBusiness.insert(testEntity6, mockAutheticatedOwner.get)
    testEntity7WithId = agoraBusiness.insert(testEntity7, mockAutheticatedOwner.get)
    testEntityToBeRedactedWithId = agoraBusiness.insert(testEntityToBeRedacted, mockAutheticatedOwner.get)
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "return information about a method, including metadata " in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get) ~> methodsService.querySingleRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => assert(entity === testEntity1WithId))
      assert(status === OK)
    }
  }

  "Agora" should "return only payload in plain/text when parameter is set " in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get + "?onlyPayload=true") ~> methodsService.querySingleRoute ~>
      check {
        assert(status === OK)
        assert(mediaType === `text/plain`)
      }
  }

  "Agora" should "return status 404, mediaType json when nothing matches query by namespace, name, snapshotId" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/foofoofoofoo/foofoofoo/99999"
    ) ~> methodsService.querySingleRoute ~> check {
      assert(status === NotFound)
    }
  }

  "Agora" should "return methods matching query by namespace and name" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?namespace=" + namespace1.get + "&name=" + name2.get) ~>
      methodsService.queryRoute ~> check {
      handleError(
        entity.as[Seq[AgoraEntity]],
        (entities: Seq[AgoraEntity]) =>
          assert(entities.toSet === brief(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)).toSet)
      )
      assert(status === OK)
    }
  }

  "Agora" should "return methods matching query by synopsis and documentation" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?synopsis=" + uriEncode(synopsis1.get) + "&documentation=" +
      uriEncode(documentation1.get)) ~>
      methodsService.queryRoute ~>
      check {
        handleError(
          entity.as[Seq[AgoraEntity]],
          (entities: Seq[AgoraEntity]) =>
            assert(entities.toSet === brief(Seq(testEntity1WithId, testEntity2WithId, testEntity3WithId, testEntity6WithId, testEntity7WithId)).toSet)
        )
        assert(status === OK)
      }
  }


  "Agora" should "return methods matching query by owner and payload" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?owner=" + owner1.get + "&payload=" + uriEncode(payload1.get)) ~>
      methodsService.queryRoute ~>
      check {
        handleError(
          entity.as[Seq[AgoraEntity]],
          (entities: Seq[AgoraEntity]) =>
            assert(entities.toSet === brief(Seq(testEntity1WithId, testEntity3WithId, testEntity4WithId, testEntity5WithId)).toSet)
        )
      }
  }

  "Agora" should "create a method and return with a status of 201" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === namespace3)
        assert(entity.name === name1)
        assert(entity.synopsis === synopsis1)
        assert(entity.documentation === documentation1)
        assert(entity.owner === owner1)
        assert(entity.payload === payload1)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
      assert(status === Created)
    }
  }

  "Agora" should "return a 400 bad request when posting a malformed payload" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntity) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "Agora" should "return a 400 bad request with validation errors when metadata is invalid" in {
    val entityJSON = s"""{
                        | "namespace": "  ",
                        | "name": "  ",
                        | "synopsis": " ",
                        | "documentation": "",
                        | "payload": "",
                        | "entityType": "Task"
                        |}""".stripMargin

    val entity = HttpEntity(
      contentType = ContentType(`application/json`),
      string = entityJSON)

    Post(ApiUtil.Methods.withLeadingVersion, entity) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(status === BadRequest)
    }
  }

  "Agora" should "store 10kb of github markdown as method documentation and return it without alteration" in {
    val entityJSON = s"""{
                        | "namespace": "$namespace1",
                        | "name": "$name1",
                        | "synopsis": "",
                        | "documentation": "$getBigDocumentation",
                        | "payload": "",
                        | "entityType": "Task"
                        |}""".stripMargin

    val entity = HttpEntity(
      contentType = ContentType(`application/json`),
      string = entityJSON)

    Post(ApiUtil.Methods.withLeadingVersion, entity) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(status === BadRequest)
    }
  }

  "Agora" should "not allow you to post a new configuration to the methods route" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraConfigurationEntity) ~>
      methodsService.postRoute ~> check {
      rejection === ValidationRejection
    }
  }

  "Agora" should "allow method redaction" in {
    Delete(ApiUtil.Methods.withLeadingVersion + "/" + testEntityToBeRedactedWithId.namespace.get + "/" +
      testEntityToBeRedactedWithId.name.get + "/" + testEntityToBeRedactedWithId.snapshotId.get) ~>
    methodsService.querySingleRoute ~> check {
      assert(body.asString === "1")
    }
  }

  "Agora" should "not allow redacted methods to be queried" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + testEntityToBeRedactedWithId.namespace.get + "/" +
      testEntityToBeRedactedWithId.name.get + "/" + testEntityToBeRedactedWithId.snapshotId.get) ~>
      methodsService.querySingleRoute ~> check {
      assert(body.asString contains "not found")
    }
  }
}
