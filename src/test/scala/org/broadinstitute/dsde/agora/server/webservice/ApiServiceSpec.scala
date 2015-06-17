package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.authorization.{TestAuthorizationProvider, TestAuthorizationProvider$}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.validation.AgoraValidationRejection
import org.scalatest.{DoNotDiscover, _}
import spray.http.StatusCodes._
import spray.httpx.unmarshalling._
import spray.routing.{Directives, RejectionHandler}
import spray.testkit.ScalatestRouteTest

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Directives with ScalatestRouteTest with AgoraTestData with BeforeAndAfterAll {
  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  val agoraBusiness = new AgoraBusiness(TestAuthorizationProvider)

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
    testEntity1WithId = agoraBusiness.findSingle(testEntity1, agoraCIOwner.get).get
    testEntity2WithId = agoraBusiness.findSingle(testEntity2, agoraCIOwner.get).get
    testEntity3WithId = agoraBusiness.findSingle(testEntity3, agoraCIOwner.get).get
    testEntity4WithId = agoraBusiness.findSingle(testEntity4, agoraCIOwner.get).get
    testEntity5WithId = agoraBusiness.findSingle(testEntity5, agoraCIOwner.get).get
    testEntity6WithId = agoraBusiness.findSingle(testEntity6, agoraCIOwner.get).get
    testEntity7WithId = agoraBusiness.findSingle(testEntity7, agoraCIOwner.get).get

    testEntityTaskWcWithId = agoraBusiness.findSingle(testEntityTaskWc, agoraCIOwner.get).get
    testConfigurationEntityWithId = agoraBusiness.findSingle(testAgoraConfigurationEntity, agoraCIOwner.get).get
  }

  val methodsService = new MethodsService(TestAuthorizationProvider) with ActorRefFactoryContext with AgoraOpenAMMockDirectives
  val configurationsService = new ConfigurationsService(TestAuthorizationProvider) with ActorRefFactoryContext with AgoraOpenAMMockDirectives

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
        url = Option(agoraBusiness.agoraUrl(entity)),
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
        url = Option(agoraBusiness.agoraUrl(entity)),
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
        url = Option(agoraBusiness.agoraUrl(entity))
      )
    )
  }
}

