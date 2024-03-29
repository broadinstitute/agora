
package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created}
import akka.http.scaladsl.model.HttpMethods.{DELETE, GET}
import akka.http.scaladsl.server.{MethodRejection, PathMatcher, Route}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.model.headers.CacheDirectives.`no-cache`
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraConfig.authenticationDirectives
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessExecutionContext}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.exceptions.AgoraEntityNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.routes.RouteHelpers

import scala.util.{Failure, Success, Try}
import cats.data.Validated._

import scala.concurrent.ExecutionContext

/**
 * AgoraService defines routes for ApiServiceActor.
 *
 * Concrete implementations are MethodsService and ConfigurationsService.
 */
abstract class AgoraService(permissionsDataSource: PermissionsDataSource) extends RouteHelpers {

  private val agoraBusiness = new AgoraBusiness(permissionsDataSource)

  def path: String

  final def routes(implicit
                   executionContext: ExecutionContext,
                   agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Route =
    respondWithHeaders(`Cache-Control`(`no-cache`)) {
      queryAssociatedConfigurationsRoute ~ queryCompatibleConfigurationsRoute ~ querySingleRoute ~
        queryMethodDefinitionsRoute ~ queryRoute ~ postRoute
    }

  // GET http://root.com/methods/<namespace>/<name>/<snapshotId>?onlyPayload=true
  // GET http://root.com/configurations/<namespace>/<name>/<snapshotId>
  def querySingleRoute(implicit
                       ec: ExecutionContext,
                       agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Route =
    matchQuerySingleRoute(path)(ec) {
      (namespace, name, snapshotId, username, accessToken) =>
        parameters( "onlyPayload".as[Boolean] ? false, "payloadAsObject".as[Boolean] ? false ) {
          (onlyPayload, payloadAsObject) =>
            val targetEntity = AgoraEntity(Option(namespace), Option(name), Option(snapshotId))
            val entityTypes = AgoraEntityType.byPath(path)

            get {
              Try(agoraBusiness.findSingle(targetEntity, entityTypes, username)(
                agoraBusinessExecutionContext.executionContext
              )) match {
                case Success(queryAttempt) =>
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
                case Failure(nfe:AgoraEntityNotFoundException) => failWith(nfe) // expected not-found exception
                case Failure(ex) => failWith(ex) // unexpected exception thrown when querying for entity

              }
            } ~
            delete {
              complete(agoraBusiness.delete(targetEntity, entityTypes, username).map(_.toString)(
                agoraBusinessExecutionContext.executionContext
              ))
            } ~
            post {
              // only allow copying (post) for methods
              if (path != AgoraConfig.methodsRoute) {
                reject(MethodRejection(GET), MethodRejection(DELETE))
              } else {
                entity(as[AgoraEntity]) { newEntity =>
                  AgoraEntity.validate(newEntity) match {
                    case Valid(_) => //noop
                    case Invalid(errors) => throw new IllegalArgumentException(s"Method is invalid: Errors: $errors")
                  }
                  parameters("redact".as[Boolean] ? false) { redact =>
                    complete(agoraBusiness.copy(targetEntity, newEntity, redact, username, accessToken)(
                      agoraBusinessExecutionContext.executionContext
                    ))
                  }
                }
              }
            }
        }
    }

  def queryMethodDefinitionsRoute(implicit
                                  ec: ExecutionContext,
                                  agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Route =
    (versionedPath(PathMatcher("methods" / "definitions")) & get &
      authenticationDirectives.usernameFromRequest(())) { username =>
        complete(agoraBusiness.listDefinitions(username)(
          agoraBusinessExecutionContext.executionContext
        ))
    }

  // all configurations that reference any snapshot of the supplied method
  def queryAssociatedConfigurationsRoute(implicit
                                         ec: ExecutionContext,
                                         agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Route =
    (versionedPath(PathMatcher("methods" / Segment / Segment / "configurations")) & get &
      authenticationDirectives.usernameFromRequest(())) { (namespace, name, username) =>
        complete(agoraBusiness.listAssociatedConfigurations(namespace, name, username)(
          agoraBusinessExecutionContext.executionContext
        ))
    }

  // all configurations that have the same inputs and outputs of the supplied method snapshot,
  // as well as referencing any snapshot of the supplied method
  def queryCompatibleConfigurationsRoute(implicit
                                         ec: ExecutionContext,
                                         agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Route =
    (versionedPath(PathMatcher("methods" / Segment / Segment / IntNumber / "configurations")) & get &
      authenticationDirectives.usernameFromRequest(()) & authenticationDirectives.tokenFromRequest()) { (namespace, name, snapshotId, username, accessToken) =>
        complete(agoraBusiness.listCompatibleConfigurations(namespace, name, snapshotId, username, accessToken)(
          agoraBusinessExecutionContext.executionContext
        ))
    }

  // GET http://root.com/methods?
  // GET http://root.com/configurations?
  def queryRoute(implicit
                 ec: ExecutionContext,
                 agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Route =
    matchQueryRoute(path)(ec) { username =>
      parameterMultiMap { params =>
        validateEntityType(params, path) {
          val entity = entityFromParams(params)
          val projection = projectionFromParams(params)
          val entityTypes = AgoraEntityType.byPath(path)

          complete(agoraBusiness.find(entity, projection, entityTypes, username)(
            agoraBusinessExecutionContext.executionContext
          ))
        }
      }
    }

  // POST http://root.com/methods
  // POST http://root.com/configurations
  def postRoute(implicit
                ec: ExecutionContext,
                agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Route =
    postPath(path)(ec) { (username, accessToken) =>
      entity(as[AgoraEntity]) { agoraEntity =>
        validatePostRoute(agoraEntity, path) {
          val entityWithType = AgoraEntityType.byPath(path) match {
            case AgoraEntityType.Configuration => agoraEntity.addEntityType(Option(AgoraEntityType.Configuration))
            case _ => agoraEntity
          }

          complete(Created, agoraBusiness.insert(entityWithType, username, accessToken)(
            agoraBusinessExecutionContext.executionContext
          ))
        }
      }
    }
}
