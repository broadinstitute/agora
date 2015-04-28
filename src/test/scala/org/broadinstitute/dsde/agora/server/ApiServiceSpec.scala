package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.util.AgoraTestUtil
import org.broadinstitute.dsde.agora.server.webservice.{ApiJsonSupport, TasksQueryResponse}
import org.broadinstitute.dsde.agora.server.webservice.tasks.TasksService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import ApiJsonSupport._
import org.scalatest.{FlatSpec, Matchers}
import spray.routing.Directives
import spray.testkit.ScalatestRouteTest

class ApiServiceSpec extends FlatSpec with Matchers with Directives with ScalatestRouteTest {
  def actorRefFactory = system

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val tasksService = new TasksService with ActorRefFactoryContext with MockServiceHandlerProps

  "Agora" should "return information about a task/workflow, including metadata " in {
    Get(ApiUtil.Tasks.withLeadingSlash + "/" + AgoraTestUtil.testVaultId) ~> tasksService.queryRoute ~> check {
      responseAs[TasksQueryResponse] === AgoraTestUtil.testTask(AgoraTestUtil.testVaultId)
    }
  }
}
