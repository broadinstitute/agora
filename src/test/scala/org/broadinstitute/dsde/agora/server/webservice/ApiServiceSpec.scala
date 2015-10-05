package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.scalatest.{DoNotDiscover, _}
import spray.http.StatusCodes._
import spray.httpx.unmarshalling._
import spray.routing.{Directives, ExceptionHandler, MalformedRequestContentRejection, RejectionHandler}
import spray.testkit.ScalatestRouteTest

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Directives with ScalatestRouteTest with AgoraTestFixture {

  val agoraBusiness = new AgoraBusiness()

  val wrapWithExceptionHandler = handleExceptions(ExceptionHandler {
    case e: IllegalArgumentException => complete(BadRequest, e.getMessage)
  })

  val wrapWithRejectionHandler = handleRejections(RejectionHandler {
    case MalformedRequestContentRejection(message, cause) :: _ => complete(BadRequest, message)
  })

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService() with ActorRefFactoryContext
  val configurationsService = new ConfigurationsService() with ActorRefFactoryContext

  def handleError[T](deserialized: Deserialized[T], assertions: (T) => Unit) = {
    if (status.isSuccess) {
      if (deserialized.isRight) assertions(deserialized.right.get) else failTest(deserialized.left.get.toString)
    } else {
      failTest(response.message.toString)
    }
  }

  def uriEncode(uri: String): String = {
    java.net.URLEncoder.encode(uri, "UTF-8")
  }

  def brief(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        synopsis = entity.synopsis,
        owner = entity.owner,
        url = entity.url,
        createDate = entity.createDate,
        entityType = entity.entityType,
        id = entity.id
      )
    )
  }

  def brief(agoraEntity: AgoraEntity): AgoraEntity = {
    brief(Seq(agoraEntity)).head
  }

  def excludeProjection(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(
        namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        owner = entity.owner,
        url = Option(entity.agoraUrl),
        entityType = entity.entityType
      )
    )
  }

  def includeProjection(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(
        namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        entityType = entity.entityType,
        url = Option(entity.agoraUrl)
      )
    )
  }
}

