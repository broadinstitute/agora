package org.broadinstitute.dsde.agora.server.webservice.permissions

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraConfig.authenticationDirectives
import org.broadinstitute.dsde.agora.server.business.PermissionBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions.Nothing
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.routes.{BaseRoute, RouteHelpers}
import spray.json.DefaultJsonProtocol

import scala.concurrent.ExecutionContext

class EntityPermissionsService(dataSource: PermissionsDataSource) extends RouteHelpers with BaseRoute with Directives with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  lazy val permissionBusiness = new PermissionBusiness(dataSource)

  def routes(implicit executionContext: ExecutionContext): Route =
    authenticationDirectives.usernameFromRequest(()) { username: String =>
      path("api" / AgoraConfig.version / ("configurations" | "methods") / Segment / Segment / IntNumber / "permissions") {
        (namespace: String, name: String, snapshotId: Int) =>
        parameterMap { params =>
          val agoraEntity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))

          entity(as[List[AccessControl]]) { acl =>
            post {
              completeVia(acl)(permissionBusiness.batchEntityPermission(agoraEntity, username, acl))
            }
          } ~
          get {
            complete(permissionBusiness.listEntityPermissions(agoraEntity, username))
          } ~
          post {
            val accessObject = AccessControl.fromParams(params)
            completeVia(accessObject)(permissionBusiness.insertEntityPermission(agoraEntity, username, accessObject))
          } ~
          put {
            val accessObject = AccessControl.fromParams(params)
            completeVia(accessObject)(permissionBusiness.editEntityPermission(agoraEntity, username, accessObject))
          } ~
          delete {
            val userToRemove = getUserFromParams(params)
            val accessControl = AccessControl(userToRemove, AgoraPermissions(Nothing))
            completeVia(accessControl)(permissionBusiness.deleteEntityPermission(agoraEntity, username, userToRemove))
          }
        }
      }
    }

}
