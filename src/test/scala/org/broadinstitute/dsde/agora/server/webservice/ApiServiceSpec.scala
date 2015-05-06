package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.model.AgoraEntity._
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps}
import org.broadinstitute.dsde.agora.server.{AgoraDbTest, AgoraTestData}
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}
import spray.http.StatusCodes._
import spray.httpx.marshalling._
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest with AgoraTestData with AgoraDbTest {

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with ServiceHandlerProps

  "Agora" should "return information about a method, including metadata " in {
    val insertedEntity = agoraDao.insert(testEntity)

    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace + "/" + name + "/"
      + insertedEntity.id.get) ~> methodsService.queryRoute ~> check {
      responseAs[AgoraEntity] === insertedEntity
    }
  }

  "Agora" should "create and return a method" in {
    Post(ApiUtil.Methods.withLeadingSlash, marshal(testAddRequest)) ~> methodsService.postRoute ~> check {
      val response = responseAs[AgoraEntity]
      response.namespace === namespace
      response.name === name
      response.synopsis === synopsis
      response.documentation === documentation
      response.owner === owner
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

}
