package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.util.AgoraTestUtil
import org.broadinstitute.dsde.agora.server.webservice.ApiJsonSupport._
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.broadinstitute.dsde.agora.server.webservice.{ApiJsonSupport, MethodsQueryResponse}
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest

class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest {
  def actorRefFactory = system

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with MockServiceHandlerProps

  "Agora" should "return information about a method, including metadata " in {
    Get(ApiUtil.Methods.withLeadingSlash + "/" + AgoraTestUtil.testVaultId) ~> methodsService.queryRoute ~> check {
      responseAs[MethodsQueryResponse] === AgoraTestUtil.testMethod(AgoraTestUtil.testVaultId)
    }
  }
}
