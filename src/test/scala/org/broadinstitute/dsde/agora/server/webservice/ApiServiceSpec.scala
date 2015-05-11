package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps}
import org.broadinstitute.dsde.agora.server.{AgoraDbTest, AgoraTestData}
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest
with AgoraTestData with AgoraDbTest {

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with ServiceHandlerProps

  "Agora" should "return information about a method, including metadata " in {
    val insertedEntity = agoraDao.insert(testEntity1)

    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace1.get + "/" + name1.get + "/"
      + insertedEntity.id.get) ~> methodsService.queryByNamespaceNameIdRoute ~> check {
      handleDeserializationErrors(entity.as[AgoraEntity], (entity: AgoraEntity) => assert(entity === insertedEntity))
    }
  }

  "Agora" should "return methods matching query by namespace and name" in {
    agoraDao.insert(testEntity2)
    agoraDao.insert(testEntity3)
    agoraDao.insert(testEntity4)
    agoraDao.insert(testEntity5)
    agoraDao.insert(testEntity6)
    agoraDao.insert(testEntity7)
    Get(ApiUtil.Methods.withLeadingSlash + "?namespace=" + namespace1.get + "&name=" + name2.get) ~>
      methodsService.queryRoute ~> check {
      handleDeserializationErrors(
        entity.as[Seq[AgoraEntity]],
        (entities: Seq[AgoraEntity]) =>
          assert(entities === Seq(testEntity3, testEntity4, testEntity5, testEntity6, testEntity7))
      )
    }
  }

  "Agora" should "return methods matching query by synopsis and documentation" in {
    Get(ApiUtil.Methods.withLeadingSlash + "?synopsis=" + uriEncode(synopsis1.get) + "&documentation=" +
      uriEncode(documentation1.get)) ~> methodsService.queryRoute ~> check {
      handleDeserializationErrors(
        entity.as[Seq[AgoraEntity]],
        (entities: Seq[AgoraEntity]) =>
          assert(entities === Seq(testEntity1, testEntity2, testEntity3, testEntity6, testEntity7))
      )
    }
  }

  "Agora" should "return methods matching query by owner and payload" in {
    Get(ApiUtil.Methods.withLeadingSlash + "?owner=" + owner1.get + "&payload=" + uriEncode(payload1.get)) ~>
      methodsService.queryRoute ~> check {
      handleDeserializationErrors(
        entity.as[Seq[AgoraEntity]],
        (entities: Seq[AgoraEntity]) =>
          assert(entities === Seq(testEntity1, testEntity2, testEntity3, testEntity4, testEntity5))
      )
    }
  }

  "Agora" should "create and return a method" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntity) ~> methodsService.postRoute ~> check {
      handleDeserializationErrors(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === namespace1)
        assert(entity.name === name1)
        assert(entity.synopsis === synopsis1)
        assert(entity.documentation === documentation1)
        assert(entity.owner === owner1)
        assert(entity.payload === payload1)
        assert(entity.id !== None)
        assert(entity.createDate != null)
      }
      )
    }
  }

  "Agora" should "return a 400 bad request when posting a malformed payload" in {
    Post(ApiUtil.Methods.withLeadingSlash, testBadAgoraEntity) ~> methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "Agora" should "store 10kb of github markdown as method documentation and return it without alteration" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityBigDoc) ~> methodsService.postRoute ~> check {
      handleDeserializationErrors(
        entity.as[AgoraEntity],
        (entity: AgoraEntity) => assert(entity.documentation.get === bigDocumentation)
      )
    }
  }

  def handleDeserializationErrors[T](deserialized: Deserialized[T], assertions: (T) => Unit) = {
    if (deserialized.isRight) assertions else failTest(deserialized.left.get.toString)
  }

  def uriEncode(uri: String): String = {
    java.net.URLEncoder.encode(uri, "UTF-8")
  }
}