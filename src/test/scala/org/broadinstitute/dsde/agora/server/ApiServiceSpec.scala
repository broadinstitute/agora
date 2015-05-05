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
import spray.http.StatusCodes._

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
  val payload = """task wc {
                  |  command {
                  |    echo "${str}" | wc -c
                  |  }
                  |  output {
                  |    int count = read_int("stdout") - 1
                  |  }
                  |}
                  |
                  |workflow wf {
                  |  array[string] str_array
                  |  scatter(s in str_array) {
                  |    call wc{input: str=s}
                  |  }
                  |}""".stripMargin
  val testEntity = AgoraEntity(namespace = Option(namespace), name = Option(name))
  val testAddRequest = new AgoraAddRequest(
    namespace = namespace,
    name = name,
    synopsis = synopsis,
    documentation = documentation,
    owner = owner,
    payload = payload
  )

  val badPayload = "task test {"
  val testBadAddRequest = new AgoraAddRequest(
    namespace = namespace,
    name = name,
    synopsis = synopsis,
    documentation = documentation,
    owner = owner,
    payload = badPayload
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

  "Agora" should "return a 400 bad request when posting a malformed payload" in {
    val testFixture = fixture

    Post(ApiUtil.Methods.withLeadingSlash, marshal(testBadAddRequest)) ~> methodsService.postRoute ~> check {
      status === BadRequest
      responseAs[String] != null
    }
  }

}
