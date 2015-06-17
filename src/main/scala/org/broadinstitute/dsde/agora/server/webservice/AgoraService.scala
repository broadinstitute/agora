
package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.business.AuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.broadinstitute.dsde.agora.server.webservice.validation.{AgoraValidation, AgoraValidationRejection}
import org.joda.time.DateTime
import spray.routing.HttpService

abstract class AgoraService(authorizationProvider: AuthorizationProvider) extends HttpService with PerRequestCreator with AgoraDirectives {
  private implicit val executionContext = actorRefFactory.dispatcher

  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  def path: String

  def routes = queryByNamespaceNameSnapshotIdRoute ~ queryRoute ~ postRoute

  def queryHandlerProps = Props(classOf[QueryHandler], authorizationProvider)

  def addHandlerProps = Props(classOf[AddHandler], authorizationProvider)

  def queryByNamespaceNameSnapshotIdRoute =
    path(path / Segment / Segment / Segment) { (namespace, name, snapshotId) =>
      get {
        usernameFromCookie() { userName =>
          requestContext =>
            perRequest(
              requestContext,
              queryHandlerProps,
              ServiceMessages.QueryByNamespaceNameSnapshotId(requestContext,
                namespace,
                name,
                snapshotId.toInt,
                AgoraEntityType.byPath(path),
                userName
              )
            )
        }
      }
    }

  def queryRoute =
    path(path) {
      get {
        usernameFromCookie() { userName =>
          parameters(
            "namespace".?,
            "name".?,
            "snapshotId".as[Int].?,
            "synopsis".?,
            "documentation".?,
            "owner".?,
            "createDate".as[DateTime].?,
            "payload".?,
            "url".?,
            "entityType".as[AgoraEntityType.EntityType].?
          ).as(AgoraEntity) {
            agoraEntity => {
              parameterMultiMap { params =>
                val includeFields = params.getOrElse("includedField", Seq.empty[String])
                val excludeFields = params.getOrElse("excludedField", Seq.empty[String])
                val parameterValidation = AgoraValidation.validateParameters(includeFields, excludeFields)
                val entityValidation = AgoraValidation.validateEntityType(agoraEntity.entityType, path)
                parameterValidation.valid && entityValidation.valid match {
                  case false => reject(AgoraValidationRejection(Seq(parameterValidation, entityValidation)))
                  case true =>
                    requestContext =>
                      val agoraProjection = new AgoraEntityProjection(includeFields, excludeFields)
                      val agoraProjectionOption = agoraProjection.totalFields match {
                        case 0 => None
                        case _ => Some(agoraProjection)
                      }

                      //if an entity type is specified we should search only on that type. If a type is not specified
                      //we need to search for all valid types for the given path
                      val searchTypes: Seq[AgoraEntityType.EntityType] = agoraEntity.entityType match {
                        case Some(entityType) => Seq(entityType)
                        case None => AgoraEntityType.byPath(path)
                      }
                      perRequest(
                        requestContext,
                        queryHandlerProps,
                        ServiceMessages.Query(requestContext, agoraEntity, agoraProjectionOption, searchTypes, userName)
                      )
                }
              }
            }
          }
        }
      }
    }

  def postRoute =
    path(path) {
      post {
        usernameFromCookie() { userName =>
          entity(as[AgoraEntity]) { agoraEntity =>
            val metadataValidation = AgoraValidation.validateMetadata(agoraEntity)
            val entityValidation = AgoraValidation.validateEntityType(agoraEntity.entityType, path)
            metadataValidation.valid && entityValidation.valid match {
              case false => reject(AgoraValidationRejection(Seq(metadataValidation, entityValidation)))
              case true =>
                requestContext =>
                  val entityWithOwner = agoraEntity.copy(owner = Option(userName))
                  perRequest(
                    requestContext,
                    addHandlerProps,
                    ServiceMessages.Add(requestContext, entityWithOwner, userName)
                  )
            }
          }
        }
      }
    }
}
