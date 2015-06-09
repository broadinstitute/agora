
package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.broadinstitute.dsde.agora.server.webservice.validation.{AgoraValidation, AgoraValidationRejection}
import org.joda.time.DateTime
import spray.routing.HttpService

trait AgoraService extends HttpService with PerRequestCreator with AgoraDirectives {

  private implicit val executionContext = actorRefFactory.dispatcher

  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  def path: String

  def routes = queryByNamespaceNameSnapshotIdRoute ~ queryRoute ~ postRoute

  def queryHandlerProps = Props(new QueryHandler())

  def addHandlerProps = Props(new AddHandler())

  def queryByNamespaceNameSnapshotIdRoute =
    path(path / Segment / Segment / Segment) { (namespace, name, snapshotId) =>
      get {
        requestContext =>
          perRequest(
            requestContext,
            queryHandlerProps,
            ServiceMessages.QueryByNamespaceNameSnapshotId(requestContext,
              namespace,
              name,
              snapshotId.toInt,
              AgoraEntityType.byPath(path)
            )
          )
      }
    }

  def queryRoute =
    path(path) {
      get {
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
              val validation = AgoraValidation.validateIncludeExcludeFields(includeFields, excludeFields)
              validation.valid match {
                case false => reject(AgoraValidationRejection(validation))
                case true =>
                  requestContext =>
                    val agoraProjection = new AgoraEntityProjection(includeFields, excludeFields)
                    val agoraProjectionOption = agoraProjection.totalFields match {
                      case 0 => None
                      case _ => Some(agoraProjection)
                    }
                    perRequest(
                      requestContext,
                      queryHandlerProps,
                      ServiceMessages.Query(requestContext, agoraEntity, agoraProjectionOption)
                    )
              }
            }
          }
        }
      }
    }

  def postRoute =
    path(path) {
      post {
        usernameFromCookie() { commonName =>
          entity(as[AgoraEntity]) { agoraEntity =>
            val validation = AgoraValidation.validateMetadata(agoraEntity)
            validation.valid match {
              case false => reject(AgoraValidationRejection(validation))
              case true =>
                requestContext =>
                  val entityWithOwner = agoraEntity.copy(owner = Option(commonName))
                  perRequest(
                    requestContext,
                    addHandlerProps,
                    ServiceMessages.Add(requestContext, entityWithOwner)
                  )
            }
          }
        }
      }
    }
}
