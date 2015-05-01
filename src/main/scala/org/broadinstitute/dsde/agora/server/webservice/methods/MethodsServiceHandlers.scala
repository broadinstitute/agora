package org.broadinstitute.dsde.agora.server.webservice.methods

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.webservice.MethodsQueryResponse
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.routing.RequestContext


/**
 * Actor responsible for querying unmapped bam information from the Vault
 */
class MethodsQueryHandler extends Actor {
  implicit val system = context.system

  def receive = {
    case ServiceMessages.Query(requestContext: RequestContext, id: String) =>
      query(requestContext, id)
      context.stop(self)
  }
  def query(requestContext: RequestContext, id: String): Unit = {

    // TODO- Business logic to retrieve a method from the storage implementation
    context.parent ! MethodsQueryResponse(id, Map("cwlFile" -> "fake/file/url"), Map("author" -> "Dave Shiga", "version" -> "1.0"))
  }
}