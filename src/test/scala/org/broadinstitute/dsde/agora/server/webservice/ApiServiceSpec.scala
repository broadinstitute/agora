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
      val response = grater[AgoraEntity].fromJSONArray(rawResponse)
      response === insertedEntity
    }
  }

  "Agora" should "return methods matching the query" in {
    agoraDao.insert(testEntity2)
    agoraDao.insert(testEntity3)
    agoraDao.insert(testEntity4)
    Get(ApiUtil.Methods.withLeadingSlash + "?namespace=" + namespace1 + "&owner=" + owner1) ~> methodsService.queryRoute ~> check {
      val rawResponse = responseAs[String]
      val response = grater[AgoraEntity].fromJSONArray(rawResponse)
      response === Seq(testEntity1, testEntity2)
    }
  }

  "Agora" should "create and return a method" in {
    Post(ApiUtil.Methods.withLeadingSlash, marshal(testAddRequest)) ~> methodsService.postRoute ~> check {
      val rawResponse = responseAs[String]
      val response = grater[AgoraEntity].fromJSON(rawResponse)
      response.namespace === namespace1
      response.name === name1
      response.synopsis === synopsis
      response.documentation === documentation
      response.owner === owner1
      response.payload === payload
      response.id === 1
      response.createDate != null
    }
  }

  "Agora" should "return a 400 bad request when posting a malformed payload" in {
    Post(ApiUtil.Methods.withLeadingSlash, marshal(testBadAddRequest)) ~> methodsService.postRoute ~> check {
      status === BadRequest
      responseAs[String] != null
    }
  }

  "Agora" should "store 10kb of github markdown as method documentation and return it without alteration" in {
    Post(ApiUtil.Methods.withLeadingSlash, marshal(testAddRequest)) ~> methodsService.postRoute ~> check {
      val rawResponse = responseAs[String]
      grater[AgoraEntity].fromJSON(rawResponse).documentation === bigDocumentation
    }
  }
}
