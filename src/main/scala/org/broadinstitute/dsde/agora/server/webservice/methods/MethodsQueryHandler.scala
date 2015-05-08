package org.broadinstitute.dsde.agora.server.webservice.methods

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
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
    case ServiceMessages.QueryByNamespaceNameId(requestContext: RequestContext, namespace: String, name: String, id: Int) =>
      query(requestContext, namespace, name, id)
      context.stop(self)
    case ServiceMessages.Query(requestContext: RequestContext, agoraSearch: AgoraEntity) =>
      query(requestContext, agoraSearch)
      context.stop(self)
  }

  def query(requestContext: RequestContext, namespace: String, name: String, id: Int): Unit = {
    AgoraDao.createAgoraDao.findSingle(namespace, name, id) match {
      case None => context.parent ! RequestComplete(NotFound, "Method: " + namespace + "/" + name + "/" + id + " not found")
      case Some(method) => context.parent ! RequestComplete(method)
    }
  }

  def query(requestContext: RequestContext, agoraSearch: AgoraEntity): Unit = {
    val entities = AgoraDao.createAgoraDao.find(agoraSearch)
    context.parent ! RequestComplete(entities)
  }
}
