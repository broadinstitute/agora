
package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created}
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET}
import akka.http.scaladsl.server.{MethodRejection, PathMatcher}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraConfig.authenticationDirectives
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.handlers.{AddHandler, PermissionHandler, QueryHandler}
import org.broadinstitute.dsde.agora.server.webservice.routes.RouteHelpers

import scala.util.{Failure, Success}
import scalaz.{Failure => FailureZ, Success => SuccessZ}
import spray.json._

/**
 * AgoraService defines routes for ApiServiceActor.
 *
 * Concrete implementations are MethodsService and ConfigurationsService.
 */
abstract class AgoraService(permissionsDataSource: PermissionsDataSource) extends RouteHelpers {
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global

  private val agoraBusiness = new AgoraBusiness(permissionsDataSource)

  def path: String

//  def routes = namespacePermissionsRoute ~ multiEntityPermissionsRoute ~ entityPermissionsRoute ~
//    queryAssociatedConfigurationsRoute ~ queryCompatibleConfigurationsRoute ~ querySingleRoute ~
//    queryMethodDefinitionsRoute ~ queryRoute ~ postRoute

  def routes = queryMethodDefinitionsRoute ~ queryAssociatedConfigurationsRoute ~
    queryCompatibleConfigurationsRoute ~ queryRoute ~ postRoute

  def queryHandlerProps = Props(classOf[QueryHandler], permissionsDataSource, ec)

  def addHandlerProps = Props(classOf[AddHandler], permissionsDataSource, ec)

  def permissionHandlerProps = Props(classOf[PermissionHandler], permissionsDataSource, ec)

//  def namespacePermissionsRoute =
//    matchNamespacePermissionsRoute(path) { (namespace, username) =>
//      parameterMap { (params) =>
//        val agoraEntity = AgoraEntity(Option(namespace))
//
//        // Accept batch POST // TODO: move to transactional support
//        entity(as[List[AccessControl]]) { (listOfAccessControl) =>
//          post { requestContext =>
//            completeBatchNamespacePermissionsPost(requestContext, agoraEntity, listOfAccessControl, username, permissionHandlerProps)
//          }
//        } ~
//        get { requestContext =>
//          completeNamespacePermissionsGet(requestContext, agoraEntity, username, permissionHandlerProps)
//        } ~
//        post { requestContext =>
//          completeNamespacePermissionsPost(requestContext, agoraEntity, params, username, permissionHandlerProps)
//        } ~
//        put { requestContext =>
//          completeNamespacePermissionsPut(requestContext, agoraEntity, params, username, permissionHandlerProps)
//        } ~
//        delete { requestContext =>
//          completeNamespacePermissionsDelete(requestContext, agoraEntity, params, username, permissionHandlerProps)
//        }
//
//      }
//    }
//
//  def entityPermissionsRoute =
//    matchEntityPermissionsRoute(path) { (namespace, name, snapshotId, username) =>
//      parameterMap { (params) =>
//        val agoraEntity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))
//
//        // Accept batch POST // TODO: move to transactional support
//        entity(as[List[AccessControl]]) { (listOfAccessControl) =>
//          post { requestContext =>
//            completeBatchEntityPermissionsPost(requestContext, agoraEntity, listOfAccessControl, username, permissionHandlerProps)
//          }
//        } ~
//        get { requestContext =>
//          completeEntityPermissionsGet(requestContext, agoraEntity, username, permissionHandlerProps)
//        } ~
//        post { requestContext =>
//          completeEntityPermissionsPost(requestContext, agoraEntity, params, username, permissionHandlerProps)
//        } ~
//        put { requestContext =>
//          completeEntityPermissionsPut(requestContext, agoraEntity, params, username, permissionHandlerProps)
//        } ~
//        delete { requestContext =>
//          completeEntityPermissionsDelete(requestContext, agoraEntity, params, username, permissionHandlerProps)
//        }
//
//      }
//    }
//
//  def multiEntityPermissionsRoute =
//    matchMultiEntityPermissionsRoute(path) { (username) =>
//      post {
//        entity(as[List[AgoraEntity]]) { entities => requestContext =>
//          completeMultiEntityPermissionsReport(requestContext, entities, username, permissionHandlerProps)
//        }
//      } ~
//      put {
//        entity(as[List[EntityAccessControl]]) { aclPairs => requestContext =>
//          completeMultiEntityPermissionsPut(requestContext, aclPairs, username, permissionHandlerProps)
//        }
//      }
//    }

  // GET http://root.com/methods/<namespace>/<name>/<snapshotId>?onlyPayload=true
  // GET http://root.com/configurations/<namespace>/<name>/<snapshotId>
  def querySingleRoute =
    matchQuerySingleRoute(path)(ec) {
      (namespace, name, snapshotId, username) =>
        parameters( "onlyPayload".as[Boolean] ? false, "payloadAsObject".as[Boolean] ? false ) {
          (onlyPayload, payloadAsObject) =>
            val targetEntity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))
            val entityTypes = AgoraEntityType.byPath(path)

            get {
              val queryAttempt = agoraBusiness.findSingle(targetEntity, entityTypes, username)

              onComplete(queryAttempt) {
                case Success(foundEntity) =>
                  (onlyPayload, payloadAsObject) match {
                    case (true, true) => complete(BadRequest, "onlyPayload, payloadAsObject cannot be used together")
                    case (true, false) => complete(foundEntity.payload)
                    case (false, true) => complete(foundEntity.withDeserializedPayload)
                    case _ => complete(foundEntity)
                  }
                case Failure(error) => failWith(error)
              }
            } ~
            delete {
              val deletionAttempt = agoraBusiness.delete(targetEntity, entityTypes, username)

              onComplete(deletionAttempt) {
                case Success(rowsDeleted) => complete("FIX ME")
                // TODO[GAWB-3052] The line above should be the line below; figure out what implicit may be missing
                // case Success(rowsDeleted) => complete(rowsDeleted)
                case Failure(error) => failWith(error)
              }
            } ~
            post {
              // only allow copying (post) for methods
              if (path != AgoraConfig.methodsRoute) {
                reject(MethodRejection(GET), MethodRejection(DELETE))
              } else {
                entity(as[AgoraEntity]) { newEntity =>
                  AgoraEntity.validate(newEntity) match {
                    case SuccessZ(_) => //noop
                    case FailureZ(errors) => throw new IllegalArgumentException(s"Method is invalid: Errors: $errors")
                  }
                  parameters("redact".as[Boolean] ? false) { redact =>
                    val copyingAttempt = agoraBusiness.copy(targetEntity, newEntity, redact, entityTypes, username)

                    onComplete(copyingAttempt) {
                      case Success(entities) => complete(entities)
                      case Failure(error) => failWith(error)
                    }
                  }
                }
              }
            }
        }
    }

  def queryMethodDefinitionsRoute =
    (versionedPath(PathMatcher("methods" / "definitions")) & get &
      authenticationDirectives.usernameFromRequest()) { username =>
        val listingAttempt = agoraBusiness.listDefinitions(username)

        onComplete(listingAttempt) {
          case Success(definitions) => complete(definitions)
          case Failure(error) => failWith(error)
        }
    }

  // all configurations that reference any snapshot of the supplied method
  def queryAssociatedConfigurationsRoute =
    (versionedPath(PathMatcher("methods" / Segment / Segment / "configurations")) & get &
      authenticationDirectives.usernameFromRequest()) { (namespace, name, username) =>
        val listingAttempt = agoraBusiness.listAssociatedConfigurations(namespace, name, username)

        onComplete(listingAttempt) {
          case Success(configs) => complete(configs)
          case Failure(error) => failWith(error)
        }
    }

  // all configurations that have the same inputs and outputs of the supplied method snapshot,
  // as well as referencing any snapshot of the supplied method
  def queryCompatibleConfigurationsRoute =
    (versionedPath(PathMatcher("methods" / Segment / Segment / IntNumber / "configurations")) & get &
      authenticationDirectives.usernameFromRequest()) { (namespace, name, snapshotId, username) =>
        val listingAttempt = agoraBusiness.listCompatibleConfigurations(namespace, name, snapshotId, username)

        onComplete(listingAttempt) {
          case Success(configs) => complete(configs)
          case Failure(error) => failWith(error)
        }
    }

  // GET http://root.com/methods?
  // GET http://root.com/configurations?
  def queryRoute =
    matchQueryRoute(path)(ec) { username =>
      parameterMultiMap { params =>
        validateEntityType(params, path) {
          val entity = entityFromParams(params)
          val projection = projectionFromParams(params)
          val entityTypes = AgoraEntityType.byPath(path)

          val queryAttempt = agoraBusiness.find(entity, projection, entityTypes, username)

          onComplete(queryAttempt) {
            case Success(entities) => complete(entities)
            case Failure(error) => failWith(error)
          }
        }
      }
    }

  // POST http://root.com/methods
  // POST http://root.com/configurations
  def postRoute =
    postPath(path)(ec) { username =>
      entity(as[AgoraEntity]) { agoraEntity =>
        validatePostRoute(agoraEntity, path) {
          val addHandler = new AddHandler(permissionsDataSource)
          val addAttempt = addHandler.add(agoraEntity, username, path)
          onComplete(addAttempt) {
            case Success(entity) => complete(Created, entity)
            case Failure(error) => failWith(error)
          }
        }
      }
    }
}
