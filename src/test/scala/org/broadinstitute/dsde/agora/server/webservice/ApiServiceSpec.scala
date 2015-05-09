package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps}
import org.broadinstitute.dsde.agora.server.{AgoraDbTest, AgoraTestData}
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}
import spray.http.StatusCodes._
import spray.httpx.marshalling._
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest
import com.novus.salat._
import com.novus.salat.global._

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest with AgoraTestData with AgoraDbTest {

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with ServiceHandlerProps

  "Agora" should "return information about a method, including metadata " in {
    val insertedEntity = agoraDao.insert(testEntity1)

    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace1 + "/" + name1 + "/"
      + insertedEntity.id.get) ~> methodsService.queryByNamespaceNameIdRoute ~> check {
      val rawResponse = responseAs[String]
      val response = grater[AgoraEntity].fromJSONArray(rawResponse).head
      assert(response === insertedEntity)
    }
  }

  "Agora" should "return methods matching query by namespace and name" in {
    agoraDao.insert(testEntity2)
    agoraDao.insert(testEntity3)
    agoraDao.insert(testEntity4)
    agoraDao.insert(testEntity5)
    agoraDao.insert(testEntity6)
    agoraDao.insert(testEntity7)
    Get(ApiUtil.Methods.withLeadingSlash + "?namespace=" + namespace1 + "&name=" + name2) ~> methodsService.queryRoute ~> check {
      val rawResponse = responseAs[String]
      val response = grater[AgoraEntity].fromJSONArray(rawResponse)
      assert(response === Seq(testEntity3, testEntity4, testEntity5, testEntity6, testEntity7))
    }
  }

  "Agora" should "return methods matching query by synopsis and documentation" in {
    print(uriEncode(synopsis1))
    Get(ApiUtil.Methods.withLeadingSlash + "?synopsis=" + uriEncode(synopsis1) + "&documentation=" + uriEncode(documentation1)) ~> methodsService.queryRoute ~> check {
      val rawResponse = responseAs[String]
      val response = grater[AgoraEntity].fromJSONArray(rawResponse)
      assert(response === Seq(testEntity1, testEntity2, testEntity3, testEntity6, testEntity7))
    }
  }

  "Agora" should "return methods matching query by owner and payload" in {
    Get(ApiUtil.Methods.withLeadingSlash + "?owner=" + owner1 + "&payload=" + uriEncode(payload1)) ~> methodsService.queryRoute ~> check {
      val rawResponse = responseAs[String]
      val response = grater[AgoraEntity].fromJSONArray(rawResponse)
      assert(response === Seq(testEntity1, testEntity2, testEntity3, testEntity4, testEntity5))
    }
  }

  "Agora" should "create and return a method" in {
    Post(ApiUtil.Methods.withLeadingSlash, marshal(testAddRequest)) ~> methodsService.postRoute ~> check {
      val rawResponse = responseAs[String]
      val response = grater[AgoraEntity].fromJSON(rawResponse)
      assert(response.namespace.get === namespace1)
      assert(response.name.get === name1)
      assert(response.synopsis.get === synopsis1)
      assert(response.documentation.get === documentation1)
      assert(response.owner.get === owner1)
      assert(response.payload.get === payload1)
      assert(response.id.get !== None)
      assert(response.createDate.get != null)
    }
  }

  "Agora" should "return a 400 bad request when posting a malformed payload" in {
    Post(ApiUtil.Methods.withLeadingSlash, marshal(testBadAddRequest)) ~> methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "Agora" should "store 10kb of github markdown as method documentation and return it without alteration" in {
    Post(ApiUtil.Methods.withLeadingSlash, marshal(testAddRequestBigDoc)) ~> methodsService.postRoute ~> check {
      val rawResponse = responseAs[String]
      assert(grater[AgoraEntity].fromJSON(rawResponse).documentation.get === bigDocumentation)
    }
  }

  def uriEncode(uri: String): String = {
    java.net.URLEncoder.encode(uri, "UTF-8")
  }
}
