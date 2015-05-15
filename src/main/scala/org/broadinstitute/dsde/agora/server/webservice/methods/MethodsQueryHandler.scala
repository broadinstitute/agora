package org.broadinstitute.dsde.agora.server.webservice.methods

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraError, AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * Actor responsible for querying the methods repository.
 */
class MethodsQueryHandler extends Actor {
  implicit val system = context.system

  def receive = {
    case ServiceMessages.QueryByNamespaceNameSnapshotId(requestContext: RequestContext, namespace: String, name: String, snapshotId: Int) =>
      query(requestContext, namespace, name, snapshotId)
      context.stop(self)
    case ServiceMessages.Query(requestContext: RequestContext, agoraSearch: AgoraEntity) =>
      query(requestContext, agoraSearch)
      context.stop(self)
  }

  def query(requestContext: RequestContext, namespace: String, name: String, snapshotId: Int): Unit = {
    AgoraBusiness.findSingle(namespace, name, snapshotId) match {
      case None => context.parent ! RequestComplete(NotFound, AgoraError(s"Method: ${namespace}/${name}/${snapshotId} not found"))
      case Some(method) => context.parent ! RequestComplete(method)
    }
  }

  def query(requestContext: RequestContext, agoraSearch: AgoraEntity): Unit = {
    val entities = AgoraBusiness.find(agoraSearch)
    context.parent ! RequestComplete(entities)
  }

}
