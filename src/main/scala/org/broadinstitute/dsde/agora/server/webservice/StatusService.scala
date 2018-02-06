package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.{ActorRef, Props}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.handlers.StatusHandler
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages.Status
import spray.routing.HttpService

abstract class StatusService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef) extends HttpService with PerRequestCreator {

  override implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def statusHandlerProps = Props(classOf[StatusHandler], permissionsDataSource, executionContext)

  def routes = statusRoute

  // GET /status
  def statusRoute = path("status") {
    get { requestContext =>
      perRequest(requestContext, statusHandlerProps, Status(healthMonitor))
    }
  }

}
