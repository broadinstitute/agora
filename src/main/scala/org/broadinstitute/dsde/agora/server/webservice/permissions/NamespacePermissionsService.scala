package org.broadinstitute.dsde.agora.server.webservice.permissions

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, _}
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraConfig.authenticationDirectives
import org.broadinstitute.dsde.agora.server.business.PermissionBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions.Nothing
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.routes.{BaseRoute, RouteHelpers}
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContextExecutor

class NamespacePermissionsService(dataSource: PermissionsDataSource) extends RouteHelpers with BaseRoute
  with Directives with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  // ec Required for PermissionBusiness
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  lazy val permissionBusiness = new PermissionBusiness(dataSource)(ec)

  def routes: Route =
    authenticationDirectives.usernameFromRequest(()) { username: String =>
      path("api" / AgoraConfig.version / ("configurations" | "methods") / Segment / "permissions") { namespace: String =>
        parameterMap { (params) =>
          val agoraEntity = AgoraEntity(Option(namespace))
          entity(as[List[AccessControl]]) { accessObjects =>
            post {
              completeWith(accessObjects)(permissionBusiness.batchNamespacePermission(agoraEntity, username, accessObjects))
            }
          } ~
          get {
            complete(permissionBusiness.listNamespacePermissions(agoraEntity, username))
          } ~
          post {
            val accessObject = AccessControl.fromParams(params)
            completeWith(accessObject)(permissionBusiness.insertNamespacePermission(agoraEntity, username, accessObject))
          } ~
          put {
            val accessObject = AccessControl.fromParams(params)
            completeWith(accessObject)(permissionBusiness.editNamespacePermission(agoraEntity, username, accessObject))
          } ~
          delete {
            val userToRemove = getUserFromParams(params)
            val accessControl = AccessControl(userToRemove, AgoraPermissions(Nothing))
            completeWith(accessControl)(permissionBusiness.deleteNamespacePermission(agoraEntity, username, userToRemove))
          }
        }
      }
    }
}
