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

import scala.concurrent.ExecutionContext

class NamespacePermissionsService(dataSource: PermissionsDataSource) extends RouteHelpers with BaseRoute
  with Directives with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  lazy val permissionBusiness = new PermissionBusiness(dataSource)

  def routes(implicit executionContext: ExecutionContext): Route =
    authenticationDirectives.usernameFromRequest(()) { username: String =>
      path("api" / AgoraConfig.version / ("configurations" | "methods") / Segment / "permissions") { namespace: String =>
        parameterMap { params =>
          val agoraEntity = AgoraEntity(Option(namespace))
          entity(as[List[AccessControl]]) { accessObjects =>
            post {
              completeVia(accessObjects)(permissionBusiness.batchNamespacePermission(agoraEntity, username, accessObjects))
            }
          } ~
          get {
            complete(permissionBusiness.listNamespacePermissions(agoraEntity, username))
          } ~
          post {
            val accessObject = AccessControl.fromParams(params)
            completeVia(accessObject)(permissionBusiness.insertNamespacePermission(agoraEntity, username, accessObject))
          } ~
          put {
            val accessObject = AccessControl.fromParams(params)
            completeVia(accessObject)(permissionBusiness.editNamespacePermission(agoraEntity, username, accessObject))
          } ~
          delete {
            val userToRemove = getUserFromParams(params)
            val accessControl = AccessControl(userToRemove, AgoraPermissions(Nothing))
            completeVia(accessControl)(permissionBusiness.deleteNamespacePermission(agoraEntity, username, userToRemove))
          }
        }
      }
    }
}
