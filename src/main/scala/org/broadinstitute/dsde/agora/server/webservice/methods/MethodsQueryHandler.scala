package org.broadinstitute.dsde.agora.server.webservice.methods

import akka.actor.Actor
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.broadinstitute.dsde.agora.server.webservice.PerRequest._
import spray.http.StatusCodes._
import spray.routing.RequestContext
import com.novus.salat._
import com.novus.salat.global._



/**
 * Actor responsible for querying the methods repository.
 */
class MethodsQueryHandler extends Actor {
  implicit val system = context.system

  def receive = {
    case ServiceMessages.Query(requestContext: RequestContext, namespace: String, name: String, id: Int) =>
      query(requestContext, namespace, name, id)
      context.stop(self)
  }

  def query(requestContext: RequestContext, namespace: String, name: String, id: Int): Unit = {
    AgoraDao.createAgoraDao.findSingle(namespace, name, id) match {
      case None => context.parent ! RequestComplete(NotFound, "Method: " + namespace + "/" + name + "/" + id + " not found")
      case Some(method) => context.parent ! grater[AgoraEntity].toJSON(method)
    }
  }
}