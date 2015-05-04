package org.broadinstitute.dsde.agora.server.webservice.util

import org.broadinstitute.dsde.agora.server.model.AgoraAddRequest
import spray.routing.RequestContext

/**
 * Case classes representing messages to pass to service handler actors
 */
object ServiceMessages {
  case class Query(requestContext: RequestContext, namespace: String, name: String, id: Int)
  
  case class Add(requestContext: RequestContext, agoraAddRequest: AgoraAddRequest)
}
