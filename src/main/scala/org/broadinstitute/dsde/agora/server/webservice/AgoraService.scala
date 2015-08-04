
package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AuthorizationProvider
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
abstract class AgoraService(authorizationProvider: AuthorizationProvider) extends HttpService with RouteHelpers {

  def path: String

  def routes = namespaceAclsRoute ~ entityAclsRoute ~ querySingleRoute ~ queryRoute ~ postRoute

  def queryHandlerProps = Props(classOf[QueryHandler], authorizationProvider)

  def addHandlerProps = Props(classOf[AddHandler], authorizationProvider)

  def aclHandlerProps = Props(classOf[AclHandler], authorizationProvider)

  def namespaceAclsRoute =
    matchNamespaceAclsRoute(path) { (namespace, username) =>
      parameterMap { (params) =>
        val entity = AgoraEntity(Option(namespace))

        get {requestContext =>
          completeNamespaceAclsGet(requestContext, entity, username, aclHandlerProps)
        } ~
        post {requestContext =>
          completeNamespaceAclsPost(requestContext, entity, params, username, aclHandlerProps)
        } ~
        delete {requestContext =>
          completeNamespaceAclsDelete(requestContext, entity, params, username, aclHandlerProps)
        }
    }
  }

  def entityAclsRoute =
    matchEntityAclRoute(path) { (namespace, name, snapshotId, username) =>
      parameterMap { (params) =>
        val entity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))

        get { requestContext =>
          completeEntityAclGet(requestContext, entity, username, aclHandlerProps)
        } ~
        post { requestContext =>
          completeEntityAclPost(requestContext, entity, params, username, aclHandlerProps)
        } ~
        delete {requestContext =>
          completeEntityAclDelete(requestContext, entity, params, username, aclHandlerProps)
        }
      }
    }

  // GET http://root.com/methods/<namespace>/<name>/<snapshotId>?onlyPayload=true
  // GET http://root.com/configurations/<namespace>/<name>/<snapshotId>
  def querySingleRoute =
    matchQuerySingleRoute(path) { (namespace, name, snapshotId) =>
      usernameFromCookie() { (username) =>
        extractOnlyPayloadParameter { (onlyPayload) =>
          requestContext =>
            val entity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))
            completeWithPerRequest(requestContext, entity, username, toBool(onlyPayload), path, queryHandlerProps)
        }
      }
    }

  // GET http://root.com/methods?
  // GET http://root.com/configurations?
  def queryRoute =
    matchQueryRoute(path) {
      usernameFromCookie() { username =>
        parameterMultiMap { params =>
          validateEntityType(params, path) {
            requestContext => completeWithPerRequest(requestContext, params, username, path, queryHandlerProps)
          }
        }
      }
    }

  // POST http://root.com/methods
  // POST http://root.com/configurations
  def postRoute =
    postPath(path) {
      usernameFromCookie() { username =>
        entity(as[AgoraEntity]) { agoraEntity =>
          validateEntityType(agoraEntity, path) {
            requestContext => completeWithPerRequest(requestContext, agoraEntity, username, addHandlerProps)
          }
        }
      }
    }

}


