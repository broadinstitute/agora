
package org.broadinstitute.dsde.agora.server

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.util.AgoraTestUtil
import org.broadinstitute.dsde.agora.server.webservice.ApiJsonSupport
import org.broadinstitute.dsde.agora.server.webservice.tasks.TasksQueryHandler
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceHandlerProps
import ApiJsonSupport._
import spray.routing.RequestContext

trait MockServiceHandlerProps extends ServiceHandlerProps {
  override def tasksQueryHandlerProps = Props(new MockTasksQueryHandler)
}

class MockTasksQueryHandler extends TasksQueryHandler {
  override def query(requestContext: RequestContext, id: String): Unit = requestContext.complete(AgoraTestUtil.testTask(id))
}
