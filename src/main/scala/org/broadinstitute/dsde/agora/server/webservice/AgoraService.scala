
package org.broadinstitute.dsde.agora.server.webservice



import scala.util.Try
import akka.actor.Props
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.broadinstitute.dsde.agora.server.webservice.validation.{AgoraValidation, AgoraValidationRejection}
import org.joda.time.DateTime
import spray.routing.{RequestContext, UnacceptedResponseContentTypeRejection, HttpService}
import spray.http.MediaTypes._
import spray.http.{StatusCodes, RequestProcessingException, ContentType}

trait AgoraService extends HttpService with PerRequestCreator with AgoraDirectives {

  private implicit val executionContext = actorRefFactory.dispatcher

  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  def path: String

  def routes = querySingleRoute ~ queryRoute ~ postRoute

  def queryHandlerProps = Props(new QueryHandler())

  def addHandlerProps = Props(new AddHandler())

  // Route: GET http://root.com/<namespace>/<name>/<snapshotId>
  // Route: GET http://root.com/<namespace>/<name>/<snapshotId>?onlyPayload=True
  def querySingleRoute =

    matchPath { (namespace, name, snapshotId) =>
      extractOnlyParameter { (only) =>
        val onlyBool = extractBool(only)
        requestContext =>
          completeWithPerRequest(requestContext, namespace,
                                 name, snapshotId, onlyBool)
      }
    }

  val matchPath = get & path(path / Segment / Segment / IntNumber)
  val extractOnlyParameter = extract(_.request.uri.query.get("onlyPayload"))

    def extractBool(x: Option[String]): Boolean = {
      x match {
        case Some(x) => Try(x.toBoolean).getOrElse(false)
        case None => false
      }
    }

    def completeWithPerRequest(context: RequestContext,
                               namespace: String,
                               name: String,
                               snapshotId: Int,
                               onlyPayload: Boolean): Unit = {

      val _path = AgoraEntityType.byPath(path)
      val message = ServiceMessages.QuerySingle(context, namespace, name, snapshotId, _path, onlyPayload)
      perRequest(context, queryHandlerProps, message)
    }

  // Route: GET http://root.com/methods?
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
                      ServiceMessages.Query(requestContext, agoraEntity, agoraProjectionOption, searchTypes)
                    )
              }
            }
          }
        }
      }
    }

  // Route: POST http://root.com/methods
  def postRoute =
    path(path) {
      post {
        usernameFromCookie() { commonName =>
          entity(as[AgoraEntity]) { agoraEntity =>
            val metadataValidation = AgoraValidation.validateMetadata(agoraEntity)
            val entityValidation = AgoraValidation.validateEntityType(agoraEntity.entityType, path)
            metadataValidation.valid && entityValidation.valid match {
              case false => reject(AgoraValidationRejection(Seq(metadataValidation, entityValidation)))
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
