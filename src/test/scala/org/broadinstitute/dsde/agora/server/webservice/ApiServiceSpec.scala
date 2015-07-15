package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.authorization.TestAuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.scalatest.{DoNotDiscover, _}
import spray.http.StatusCodes._
import spray.httpx.unmarshalling._
import spray.routing.{MalformedRequestContentRejection, ExceptionHandler, Directives, RejectionHandler}
import spray.testkit.ScalatestRouteTest

@DoNotDiscover
class ApiServiceSpec extends FlatSpec with Directives with ScalatestRouteTest with BeforeAndAfterAll {
  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  val agoraBusiness = new AgoraBusiness(TestAuthorizationProvider)

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
        url = entity.url,
        createDate = entity.createDate,
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

