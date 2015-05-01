
package org.broadinstitute.dsde.agora.server

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.util.AgoraTestUtil
import org.broadinstitute.dsde.agora.server.webservice.ApiJsonSupport
import org.broadinstitute.dsde.agora.server.webservice.ApiJsonSupport._
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsQueryHandler
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceHandlerProps
import spray.routing.RequestContext

trait MockServiceHandlerProps extends ServiceHandlerProps {
  override def methodsQueryHandlerProps = Props(new MockMethodsQueryHandler)
}

class MockMethodsQueryHandler extends MethodsQueryHandler {
  override def query(requestContext: RequestContext, id: String): Unit = requestContext.complete(AgoraTestUtil.testMethod(id))
}
