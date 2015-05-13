package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
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
with AgoraTestData {

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with ServiceHandlerProps

  "Agora" should "return information about a method, including metadata " in {
    val insertedEntity = AgoraBusiness.insert(testEntity1)

    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace1.get + "/" + name1.get + "/"
      + insertedEntity.snapshotId.get) ~> methodsService.queryByNamespaceNameSnapshotIdRoute ~> check {
      val agoraEntity = handleDeserializationErrors(entity.as[AgoraEntity])
      assert(agoraEntity === insertedEntity)
      assert(status === OK)
    }
  }

  "Agora" should "return methods matching query by namespace and name" in {
    AgoraBusiness.insert(testEntity2)
    AgoraBusiness.insert(testEntity3)
    AgoraBusiness.insert(testEntity4)
    AgoraBusiness.insert(testEntity5)
    AgoraBusiness.insert(testEntity6)
    AgoraBusiness.insert(testEntity7)
    Get(ApiUtil.Methods.withLeadingSlash + "?namespace=" + namespace1.get + "&name=" + name2.get) ~>
      methodsService.queryRoute ~> check {
      val entities = handleDeserializationErrors(entity.as[Seq[AgoraEntity]])
      assert(entities === brief(Seq(testEntity3, testEntity4, testEntity5, testEntity6, testEntity7)))
      assert(status === OK)
    }
  }

  "Agora" should "return methods matching query by synopsis and documentation" in {
    Get(ApiUtil.Methods.withLeadingSlash + "?synopsis=" + uriEncode(synopsis1.get) + "&documentation=" +
      uriEncode(documentation1.get)) ~> methodsService.queryRoute ~> check {
      val entities = handleDeserializationErrors(entity.as[Seq[AgoraEntity]])
      assert(entities === brief(Seq(testEntity1, testEntity2, testEntity3, testEntity6, testEntity7)))
      assert(status === OK)
    }
  }

  "Agora" should "return methods matching query by owner and payload" in {
    Get(ApiUtil.Methods.withLeadingSlash + "?owner=" + owner1.get + "&payload=" + uriEncode(payload1.get)) ~>
      methodsService.queryRoute ~> check {
      val entities = handleDeserializationErrors(entity.as[Seq[AgoraEntity]])
      assert(entities === brief(Seq(testEntity1, testEntity2, testEntity3, testEntity4, testEntity5)))
      assert(status === OK)
    }
  }

  "Agora" should "create a method and return with a status of 201" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntity) ~> methodsService.postRoute ~> check {
      val agoraEntity = handleDeserializationErrors(entity.as[AgoraEntity])
      assert(agoraEntity.namespace === namespace1)
      assert(agoraEntity.name === name1)
      assert(agoraEntity.synopsis === synopsis1)
      assert(agoraEntity.documentation === documentation1)
      assert(agoraEntity.owner === owner1)
      assert(agoraEntity.payload === payload1)
      assert(agoraEntity.snapshotId !== None)
      assert(agoraEntity.createDate != null)
      assert(status === Created)
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
      val agoraEntity = handleDeserializationErrors(entity.as[AgoraEntity])
      assert(agoraEntity.documentation === bigDocumentation)
      assert(status === Created)
    }
  }

  def handleDeserializationErrors[T](deserialized: Deserialized[T]): T = {
    if (deserialized.isRight) deserialized.right.get else failTest(deserialized.left.get.toString)
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

