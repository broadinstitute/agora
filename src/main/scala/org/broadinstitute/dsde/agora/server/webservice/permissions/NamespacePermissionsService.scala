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

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class NamespacePermissionsService(dataSource: PermissionsDataSource) extends RouteHelpers with BaseRoute
  with Directives with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  // ec Required for PermissionBusiness
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  lazy val permissionBusiness = new PermissionBusiness(dataSource)(ec)

  def routes: Route =
    authenticationDirectives.usernameFromRequest() { username: String =>
      path("api" / AgoraConfig.version / ("configurations" | "methods") / Segment / "permissions") { namespace: String =>
        parameterMap { (params) =>
          val agoraEntity = AgoraEntity(Option(namespace))
          entity(as[List[AccessControl]]) { (accessObjects) =>
            post {
              val message: Future[Int] = permissionBusiness.batchNamespacePermission(agoraEntity, username, accessObjects)
              onComplete(message) {
                case Success(m) => complete(accessObjects)
                case Failure(ex) => failWith(ex)
              }
            }
          } ~
          get {
            val message: Future[Seq[AccessControl]] = permissionBusiness.listNamespacePermissions(agoraEntity, username)
            complete(message)
          } ~
          post {
            val accessObject = AccessControl.fromParams(params)
            val message: Future[Int] = permissionBusiness.insertNamespacePermission(agoraEntity, username, accessObject)
            onComplete(message) {
              case Success(m) => complete(accessObject)
              case Failure(ex) => failWith(ex)
            }
          } ~
          put {
            val accessObject = AccessControl.fromParams(params)
            val message: Future[Int] = permissionBusiness.editNamespacePermission(agoraEntity, username, accessObject)
            onComplete(message) {
              case Success(m) => complete(accessObject)
              case Failure(ex) => failWith(ex)
            }
          } ~
          delete {
            val userToRemove = getUserFromParams(params)
            val message: Future[Int] = permissionBusiness.deleteNamespacePermission(agoraEntity, username, userToRemove)
            onComplete(message) {
              case Success(m) => complete(AccessControl(userToRemove, AgoraPermissions(Nothing)))
              case Failure(ex) => failWith(ex)
            }
          }
        }
      }
    }
}
