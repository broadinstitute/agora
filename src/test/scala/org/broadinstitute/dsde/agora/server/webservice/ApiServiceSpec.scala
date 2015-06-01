package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.validation.AgoraValidationRejection
import org.scalatest._
import spray.routing.{Directives, RejectionHandler}
import spray.testkit.ScalatestRouteTest
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.DoNotDiscover
import spray.http.StatusCodes._

import spray.httpx.unmarshalling._

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Directives with ScalatestRouteTest with AgoraTestData with BeforeAndAfterAll {

  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  val wrapWithRejectionHandler = handleRejections(RejectionHandler {
    case AgoraValidationRejection(validation) :: _ => complete(BadRequest, validation)
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

  override def beforeAll() = {
    testEntity1WithId = AgoraBusiness.findSingle(testEntity1).get
    testEntity2WithId = AgoraBusiness.findSingle(testEntity2).get
    testEntity3WithId = AgoraBusiness.findSingle(testEntity3).get
    testEntity4WithId = AgoraBusiness.findSingle(testEntity4).get
    testEntity5WithId = AgoraBusiness.findSingle(testEntity5).get
    testEntity6WithId = AgoraBusiness.findSingle(testEntity6).get
    testEntity7WithId = AgoraBusiness.findSingle(testEntity7).get

    testEntityTaskWcWithId = AgoraBusiness.findSingle(testEntityTaskWc).get
    testConfigurationEntityWithId = AgoraBusiness.findSingle(testAgoraConfigurationEntity).get
  }

  val methodsService = new MethodsService with ActorRefFactoryContext with AgoraOpenAMMockDirectives
  val configurationsService = new ConfigurationsService with ActorRefFactoryContext with AgoraOpenAMMockDirectives

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
        url = Option(AgoraBusiness.agoraUrl(entity)),
        entityType = entity.entityType
      )
    )
  }

  def excludeProjection(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(
        namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        owner = entity.owner,
        url = Option(AgoraBusiness.agoraUrl(entity)),
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
        url = Option(AgoraBusiness.agoraUrl(entity))
      )
    )
  }
}

