
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import spray.http.{HttpCharset, ContentType, HttpEntity}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.marshalling._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.ValidationRejection
import spray.json._

@DoNotDiscover
class AgoraMethodsSpec extends ApiServiceSpec {

  "Agora" should "return information about a method, including metadata " in {
    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get) ~> methodsService.querySingleRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => assert(entity === testEntity1WithId))
      assert(status === OK)
    }
  }

  "Agora" should "return only payload in plain/text when parameter is set " in {
    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get +"?onlyPayload=true") ~> methodsService.querySingleRoute ~>
    check {
      assert(status === OK)
      assert(mediaType === `text/plain`)
    }
  }

  "Agora" should "return status 404, mediaType json when nothing matches query by namespace, name, snapshotId" in {
    Get(ApiUtil.Methods.withLeadingSlash + "/foofoofoofoo/foofoofoo/99999"
    ) ~> methodsService.querySingleRoute ~> check {
      assert(status === NotFound)
      assert(mediaType === `application/json`)
    }
  }

  "Agora" should "return methods matching query by namespace and name" in {
    Get(ApiUtil.Methods.withLeadingSlash + "?namespace=" + namespace1.get + "&name=" + name2.get) ~>
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
    Get(ApiUtil.Methods.withLeadingSlash + "?synopsis=" + uriEncode(synopsis1.get) + "&documentation=" +
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
    Get(ApiUtil.Methods.withLeadingSlash + "?owner=" + owner1.get + "&payload=" + uriEncode(payload1.get)) ~>
      methodsService.queryRoute ~>
      check {
        handleError(
          entity.as[Seq[AgoraEntity]],
          (entities: Seq[AgoraEntity]) =>
            assert(entities.toSet === brief(Seq(testEntity1WithId, testEntity2WithId, testEntity3WithId, testEntity4WithId, testEntity5WithId)).toSet)
        )
      }
  }

  "Agora" should "create a method and return with a status of 201" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntity) ~>
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
    Post(ApiUtil.Methods.withLeadingSlash, testBadAgoraEntity) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "Agora" should "return a 400 bad request with validation errors when metadata is invalid" in {
    val entityJSON = s"""{
                         |  "namespace": "  ",
                         |  "name": "  ",
                         |  "synopsis": " ",
                         |  "documentation": "",
                         |  "payload": "",
                         |  "entityType": "Task"
                         |}""".stripMargin

    val entity = HttpEntity(
      contentType = ContentType(`application/json`),
      string = entityJSON)

    Post(ApiUtil.Methods.withLeadingSlash, entity) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(status === BadRequest)
    }
  }

  "Agora" should "store 10kb of github markdown as method documentation and return it without alteration" in {
    val entityJSON = s"""{
                        |  "namespace": "$namespace1",
                        |  "name": "$name1",
                        |  "synopsis": "",
                        |  "documentation": "$getBigDocumentation",
                        |  "payload": "",
                        |  "entityType": "Task"
                        |}""".stripMargin

    val entity = HttpEntity(
      contentType = ContentType(`application/json`),
      string = entityJSON)

    Post(ApiUtil.Methods.withLeadingSlash, entity) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
        assert(status === BadRequest)
    }
  }

  "Agora" should "not allow you to post a new configuration to the methods route" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraConfigurationEntity) ~>
      methodsService.postRoute ~> check {
      rejection === ValidationRejection
    }
  }
}
