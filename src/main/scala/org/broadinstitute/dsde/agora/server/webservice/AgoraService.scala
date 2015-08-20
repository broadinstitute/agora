
package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.routes.RouteHelpers
import spray.routing.{HttpService}
import spray.httpx.SprayJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._

/**
 * AgoraService defines routes for ApiServiceActor.
 *
 * Concrete implementaions are MethodsService and ConfigurationsService.
 */
abstract class AgoraService extends HttpService with RouteHelpers {

  def path: String

  def routes = namespacePermissionsRoute ~ entityPermissionsRoute ~ querySingleRoute ~ queryRoute ~ postRoute

  def queryHandlerProps = Props(classOf[QueryHandler])

  def addHandlerProps = Props(classOf[AddHandler])

  def permissionHandlerProps = Props(classOf[PermissionHandler])

  def namespacePermissionsRoute =
    matchNamespacePermissionsRoute(path) { (namespace, username) =>
      parameterMap { (params) =>
        val entity = AgoraEntity(Option(namespace))

        get {requestContext =>
          completeNamespacePermissionsGet(requestContext, entity, username, permissionHandlerProps)
        } ~
        post {requestContext =>
          completeNamespacePermissionsPost(requestContext, entity, params, username, permissionHandlerProps)
        } ~
        put {requestContext =>
          completeNamespacePermissionsPut(requestContext, entity, params, username, permissionHandlerProps)
        } ~
        delete {requestContext =>
          completeNamespacePermissionsDelete(requestContext, entity, params, username, permissionHandlerProps)
        }

      }
    }

  def entityPermissionsRoute =
    matchEntityPermissionsRoute(path) { (namespace, name, snapshotId, username) =>
      parameterMap { (params) =>
        val entity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))

        get { requestContext =>
          completeEntityPermissionsGet(requestContext, entity, username, permissionHandlerProps)
        } ~
        post { requestContext =>
          completeEntityPermissionsPost(requestContext, entity, params, username, permissionHandlerProps)
        } ~
        put { requestContext =>
          completeEntityPermissionsPut(requestContext, entity, params, username, permissionHandlerProps)
        } ~
        delete { requestContext =>
          completeEntityPermissionsDelete(requestContext, entity, params, username, permissionHandlerProps)
        }

      }
    }

  // GET http://root.com/methods/<namespace>/<name>/<snapshotId>?onlyPayload=true
  // GET http://root.com/configurations/<namespace>/<name>/<snapshotId>
  def querySingleRoute =
    matchQuerySingleRoute(path) { (namespace, name, snapshotId, username) =>
      extractOnlyPayloadParameter { (onlyPayload) =>
        requestContext =>
          val entity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))
          completeWithPerRequest(requestContext, entity, username, toBool(onlyPayload), path, queryHandlerProps)
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
}


