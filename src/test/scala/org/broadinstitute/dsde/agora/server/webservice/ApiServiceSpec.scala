package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps}
import org.scalatest._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest
import spray.http.MediaTypes._

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest
with AgoraTestData with BeforeAndAfterAll {

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  var testEntity1WithId: AgoraEntity = null
  var testEntity2WithId: AgoraEntity = null
  var testEntity3WithId: AgoraEntity = null
  var testEntity4WithId: AgoraEntity = null
  var testEntity5WithId: AgoraEntity = null
  var testEntity6WithId: AgoraEntity = null
  var testEntity7WithId: AgoraEntity = null

  override def beforeAll() = {
    testEntity1WithId = AgoraBusiness.insert(testEntity1)
    testEntity2WithId = AgoraBusiness.insert(testEntity2)
    testEntity3WithId = AgoraBusiness.insert(testEntity3)
    testEntity4WithId = AgoraBusiness.insert(testEntity4)
    testEntity5WithId = AgoraBusiness.insert(testEntity5)
    testEntity6WithId = AgoraBusiness.insert(testEntity6)
    testEntity7WithId = AgoraBusiness.insert(testEntity7)
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with ServiceHandlerProps with AgoraOpenAMMockDirectives

  "Agora" should "return information about a method, including metadata " in {
    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get) ~> methodsService.queryByNamespaceNameSnapshotIdRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => assert(entity === testEntity1WithId))
      assert(status === OK)
    }
  }

  "Agora" should "return status 404, mediaType json when nothing matches query by namespace, name, snapshotId" in {
    Get(ApiUtil.Methods.withLeadingSlash + "/foofoofoofoo/foofoofoo/99999"
    ) ~> methodsService.queryByNamespaceNameSnapshotIdRoute ~> check {
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
          assert(entities === brief(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)))
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
            assert(entities === brief(Seq(testEntity1WithId, testEntity2WithId, testEntity3WithId, testEntity6WithId, testEntity7WithId)))
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
            assert(entities === brief(Seq(testEntity1WithId, testEntity2WithId, testEntity3WithId, testEntity4WithId, testEntity5WithId)))
        )
      }
  }

  "Agora" should "create a method and return with a status of 201" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntity) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === namespace1)
        assert(entity.name === name1)
        assert(entity.synopsis === synopsis1)
        assert(entity.documentation === documentation1)
        assert(entity.owner === agoraCIOwner)
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

  "Agora" should "store 10kb of github markdown as method documentation and return it without alteration" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityBigDoc) ~>
      methodsService.postRoute ~> check {
      handleError(
        entity.as[AgoraEntity],
        (entity: AgoraEntity) => assert(entity.documentation.get === bigDocumentation.get)
      )
      assert(status === Created)
    }
  }

  def handleError[T](deserialized: Deserialized[T], assertions: (T) => Unit) = {
    if (status.isSuccess) {
      if (deserialized.isRight) assertions(deserialized.right.get) else failTest(deserialized.left.get.toString)
    } else {
      failTest(response.message.toString)
    }
  }

  def uriEncode(uri: String): String = {
    java.net.URLEncoder.encode(uri, "UTF-8")
  }

  def brief(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        synopsis = entity.synopsis,
        owner = entity.owner,
        url = Option(AgoraBusiness.agoraUrl(entity))
      )
    )
  }
}

