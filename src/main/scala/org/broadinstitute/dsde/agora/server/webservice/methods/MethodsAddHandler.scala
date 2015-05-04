package org.broadinstitute.dsde.agora.server.webservice.methods

import java.util.Date

import akka.actor.Actor
import com.novus.salat._
import com.novus.salat.global._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.{AgoraAddRequest, AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.routing.RequestContext

/**
 * Created by dshiga on 5/4/15.
 */
class MethodsAddHandler extends Actor {

  implicit val system = context.system

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraAddRequest) =>
      add(requestContext, agoraAddRequest)
      context.stop(self)
  }

  private def add(requestContext: RequestContext, agoraAddRequest: AgoraAddRequest): Unit = {
    val agoraEntity = AgoraEntity.fromAgoraAddRequest(agoraAddRequest, createDate = Option(new Date())) 
    val method = AgoraDao.createAgoraDao.insert(agoraEntity)
    context.parent ! grater[AgoraEntity].toJSON(method)
  }

}
