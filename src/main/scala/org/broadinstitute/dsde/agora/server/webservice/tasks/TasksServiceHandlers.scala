package org.broadinstitute.dsde.agora.server.webservice.tasks

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.webservice.{ApiJsonSupport, TasksQueryResponse}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.routing.RequestContext


/**
 * Actor responsible for querying unmapped bam information from the Vault
 */
class TasksQueryHandler extends Actor {
  implicit val system = context.system
  import system.dispatcher

  def receive = {
    case ServiceMessages.Query(requestContext: RequestContext, id: String) =>
      query(requestContext, id)
      context.stop(self)
  }
  def query(requestContext: RequestContext, id: String): Unit = {

    // TODO- Business logic to retrieve a task from the storage implementation
    context.parent ! TasksQueryResponse(id, Map("cwlFile" -> "fake/file/url"), Map("author" -> "Dave Shiga", "version" -> "1.0"))
  }
}