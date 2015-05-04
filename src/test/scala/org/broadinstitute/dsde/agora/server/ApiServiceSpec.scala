package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.{AgoraAddRequest, AgoraEntity}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity._
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps}
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest
import spray.httpx.marshalling._


class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest {
  def actorRefFactory = system

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with ServiceHandlerProps

  def fixture =
    new {
      val agoraDao = AgoraDao.createAgoraDao
    }

  val namespace = "broad"
  val name = "testMethod"
  val synopsis = "This is a test method"
  val documentation = "This is the documentation"
  val owner = "bob the builder"
  val payload = "echo 'hello world'"
  val testEntity = AgoraEntity(namespace = Option(namespace), name = Option(name))
  val testAddRequest = new AgoraAddRequest(
    namespace = namespace,
    name = name,
    synopsis = synopsis,
    documentation = documentation,
    owner = owner,
    payload = payload
  )
  
  "Agora" should "return information about a method, including metadata " in {
    val testFixture = fixture

    val insertedEntity = testFixture.agoraDao.insert(testEntity)

    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace + "/" + name + "/" + insertedEntity.id.get) ~> methodsService.queryRoute ~> check {
      responseAs[AgoraEntity] === insertedEntity
    }
  }
  
  "Agora" should "create and return a method" in {
    val testFixture = fixture

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
}
