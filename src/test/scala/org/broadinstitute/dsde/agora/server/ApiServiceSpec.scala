package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.model.AgoraEntity._
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, StandardServiceHandlerProps}
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest


class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest {
  def actorRefFactory = system

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with StandardServiceHandlerProps

  def fixture =
    new {
      val agoraDao = AgoraDao.createAgoraDao
    }

  "Agora" should "return information about a method, including metadata " in {
    val testFixture = fixture

    val namespace = "broad"
    val name = "testMethod"
    val testMethod = AgoraEntity(namespace = Option(namespace), name = Option(name))

    val insertedEntity = testFixture.agoraDao.insert(testMethod)

    Get(ApiUtil.Methods.withLeadingSlash + "/" + namespace + "/" + name + "/" + insertedEntity.id.get) ~> methodsService.queryRoute ~> check {
      responseAs[AgoraEntity] === insertedEntity
    }
  }
}
