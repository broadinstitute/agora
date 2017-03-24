
package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraEntityPermissionsClient, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.handlers.{AddHandler, PermissionHandler, QueryHandler, StatusHandler}
import org.broadinstitute.dsde.agora.server.webservice.routes.RouteHelpers
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages.Status
import spray.httpx.SprayJsonSupport._
import spray.routing.{HttpService, RequestContext}


/**
 * AgoraService defines routes for ApiServiceActor.
 *
 * Concrete implementations are MethodsService and ConfigurationsService.
 */
abstract class AgoraService(permissionsDataSource: PermissionsDataSource) extends HttpService with RouteHelpers {
  override implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def path: String

  def routes = namespacePermissionsRoute ~ entityPermissionsRoute ~ querySingleRoute ~ queryRoute ~ postRoute ~ statusRoute

  def queryHandlerProps = Props(classOf[QueryHandler])

  def addHandlerProps = Props(classOf[AddHandler], permissionsDataSource)

  def statusHandlerProps = Props(classOf[StatusHandler])

  def permissionHandlerProps = Props(classOf[PermissionHandler])

  def namespacePermissionsRoute =
    matchNamespacePermissionsRoute(path) { (namespace, username) =>
      parameterMap { (params) =>
        val agoraEntity = AgoraEntity(Option(namespace))

        // Accept batch POST // TODO: move to transactional support
        entity(as[List[AccessControl]]) { (listOfAccessControl) =>
          post { requestContext =>
            completeBatchNamespacePermissionsPost(requestContext, agoraEntity, listOfAccessControl, username, permissionHandlerProps)
          }
        } ~
        get { requestContext =>
          completeNamespacePermissionsGet(requestContext, agoraEntity, username, permissionHandlerProps)
        } ~
        post { requestContext =>
          completeNamespacePermissionsPost(requestContext, agoraEntity, params, username, permissionHandlerProps)
        } ~
        put { requestContext =>
          completeNamespacePermissionsPut(requestContext, agoraEntity, params, username, permissionHandlerProps)
        } ~
        delete { requestContext =>
          completeNamespacePermissionsDelete(requestContext, agoraEntity, params, username, permissionHandlerProps)
        }

      }
    }

  def entityPermissionsRoute =
    matchEntityPermissionsRoute(path) { (namespace, name, snapshotId, username) =>
      parameterMap { (params) =>
        val agoraEntity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))

        // Accept batch POST // TODO: move to transactional support
        entity(as[List[AccessControl]]) { (listOfAccessControl) =>
          post { requestContext =>
            completeBatchEntityPermissionsPost(requestContext, agoraEntity, listOfAccessControl, username, permissionHandlerProps)
          }
        } ~
        get { requestContext =>
          completeEntityPermissionsGet(requestContext, agoraEntity, username, permissionHandlerProps)
        } ~
        post { requestContext =>
          completeEntityPermissionsPost(requestContext, agoraEntity, params, username, permissionHandlerProps)
        } ~
        put { requestContext =>
          completeEntityPermissionsPut(requestContext, agoraEntity, params, username, permissionHandlerProps)
        } ~
        delete { requestContext =>
          completeEntityPermissionsDelete(requestContext, agoraEntity, params, username, permissionHandlerProps)
        }

      }
    }

  // GET http://root.com/methods/<namespace>/<name>/<snapshotId>?onlyPayload=true
  // GET http://root.com/configurations/<namespace>/<name>/<snapshotId>
  def querySingleRoute =
    matchQuerySingleRoute(path) { (namespace, name, snapshotId, username) =>
      extractOnlyPayloadParameter { (onlyPayload) =>
        val entity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))

        get { requestContext =>
          completeWithPerRequest(requestContext, entity, username, toBool(onlyPayload), path, queryHandlerProps)
        } ~
        delete { requestContext =>
          completeEntityDelete(requestContext, entity, username, path, queryHandlerProps)
        }
      }
    }

  // GET http://root.com/methods?
  // GET http://root.com/configurations?
  def queryRoute =
    matchQueryRoute(path) { (username) =>
      parameterMultiMap { params =>
        validateEntityType(params, path) {
          requestContext => completeWithPerRequest(requestContext, params, username, path, queryHandlerProps)
        }
      }
    }

  // POST http://root.com/methods
  // POST http://root.com/configurations
  def postRoute =
    postPath(path) { (username) =>
      entity(as[AgoraEntity]) { agoraEntity =>
        validatePostRoute(agoraEntity, path) {
          requestContext => completeWithPerRequest(requestContext, agoraEntity, username, path, addHandlerProps)
        }
      }
    }

  // GET /status
  def statusRoute = path("status") {
    get { requestContext =>
      perRequest(requestContext, statusHandlerProps, Status(requestContext))
    }
  }
}
