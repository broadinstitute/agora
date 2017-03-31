package org.broadinstitute.dsde.agora.server.webservice.handlers

import akka.actor.Actor
import akka.pattern._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.{PerRequestMessage, RequestComplete}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

import scala.concurrent.{ExecutionContext, Future}

/**
 * AddHandler is an actor that receives web service requests and calls AgoraBusiness logic.
 * It then handles the returns from the business layer and completes the request. It is responsible for adding a method
 * or method configuration to the methods repository.
 */
class AddHandler(dataSource: PermissionsDataSource)(implicit ec: ExecutionContext) extends Actor {
  implicit val system = context.system

  val permissionBusiness = new PermissionBusiness(dataSource)(ec)
  val agoraBusiness = new AgoraBusiness(dataSource)(ec)

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraEntity, username: String) =>
      add(requestContext, agoraAddRequest, username) pipeTo context.parent
  }

  private def add(requestContext: RequestContext, agoraEntity: AgoraEntity, username: String): Future[PerRequestMessage] = {
    permissionBusiness.addUserIfNotInDatabase(username) flatMap { _ =>
      agoraBusiness.insert(agoraEntity, username) map( RequestComplete(Created, _) )
    }
  }
}