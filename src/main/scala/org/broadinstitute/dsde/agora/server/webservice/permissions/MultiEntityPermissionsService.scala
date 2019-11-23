package org.broadinstitute.dsde.agora.server.webservice.permissions

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraConfig.authenticationDirectives
import org.broadinstitute.dsde.agora.server.business.PermissionBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{EntityAccessControl, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.routes.{BaseRoute, RouteHelpers}
import spray.json.DefaultJsonProtocol
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._

import scala.concurrent.{ExecutionContext, Future}

class MultiEntityPermissionsService(dataSource: PermissionsDataSource) extends RouteHelpers with BaseRoute
  with Directives with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  lazy val permissionBusiness = new PermissionBusiness(dataSource)

  def routes(implicit executionContext: ExecutionContext): Route =
    authenticationDirectives.usernameFromRequest(()) { username: String =>
      path("api" / AgoraConfig.version / ("configurations" | "methods") / "permissions") {
        post {
          entity(as[List[AgoraEntity]]) { entities =>
            val message: Future[Seq[EntityAccessControl]] = permissionBusiness.listEntityPermissions(entities, username)
            complete(message)
          }
        } ~
        put {
          entity(as[List[EntityAccessControl]]) { aclPairs =>
            val message: Future[Seq[EntityAccessControl]] = permissionBusiness.upsertEntityPermissions(aclPairs, username)
            complete(message)
          }
        }
      }
    }

}
