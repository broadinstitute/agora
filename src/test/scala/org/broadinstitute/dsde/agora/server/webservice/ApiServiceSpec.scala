package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData._
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
class ApiServiceSpec extends FlatSpec with Directives with ScalatestRouteTest with BeforeAndAfterAll {

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

  var testEntity1WithId: AgoraEntity = null
  var testEntity2WithId: AgoraEntity = null
  var testEntity3WithId: AgoraEntity = null
  var testEntity4WithId: AgoraEntity = null
  var testEntity5WithId: AgoraEntity = null
  var testEntity6WithId: AgoraEntity = null
  var testEntity7WithId: AgoraEntity = null
  var testEntityTaskWcWithId: AgoraEntity = null
  var testConfigurationEntityWithId: AgoraEntity = null
  var testEntityToBeRedactedWithId: Option[AgoraEntity] = null


  override def beforeAll() = {
    testEntity1WithId = agoraBusiness.find(testEntity1, None, Seq(testEntity1.entityType.get), mockAutheticatedOwner.get).head
    testEntity2WithId = agoraBusiness.find(testEntity2, None, Seq(testEntity2.entityType.get), mockAutheticatedOwner.get).head
    testEntity3WithId = agoraBusiness.find(testEntity3, None, Seq(testEntity3.entityType.get), mockAutheticatedOwner.get).head
    testEntity4WithId = agoraBusiness.find(testEntity4, None, Seq(testEntity4.entityType.get), mockAutheticatedOwner.get).head
    testEntity5WithId = agoraBusiness.find(testEntity5, None, Seq(testEntity5.entityType.get), mockAutheticatedOwner.get).head
    testEntity6WithId = agoraBusiness.find(testEntity6, None, Seq(testEntity6.entityType.get), mockAutheticatedOwner.get).head
    testEntity7WithId = agoraBusiness.find(testEntity7, None, Seq(testEntity7.entityType.get), mockAutheticatedOwner.get).head

    testEntityTaskWcWithId = agoraBusiness.find(testEntityTaskWc, None, Seq(testEntityTaskWc.entityType.get), mockAutheticatedOwner.get).head
    testConfigurationEntityWithId = agoraBusiness.find(testAgoraConfigurationEntity, None, Seq(testAgoraConfigurationEntity.entityType.get), mockAutheticatedOwner.get).head
    testEntityToBeRedactedWithId = agoraBusiness.find(testEntityToBeRedacted, None, Seq(testEntityToBeRedacted.entityType.get), mockAutheticatedOwner.get).headOption
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

