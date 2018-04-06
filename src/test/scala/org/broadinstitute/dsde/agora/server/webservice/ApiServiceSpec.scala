package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.{Directives, ExceptionHandler}
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}

import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.configurations.ConfigurationsService
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService

import org.scalatest.{DoNotDiscover, _}

import scala.concurrent.duration._

@DoNotDiscover
class ApiServiceSpec extends AgoraTestFixture with Directives with Suite with ScalatestRouteTest {

  // ScalatestRouteTest requires that it be mixed in to a thing that is of type Suite. So mix that in as well.
  // But both have run() methods, so we have to pick the right one. In other places, mixing in *Spec accomplishes this.
  override def run(testName: Option[String], args: Args): Status = super[ScalatestRouteTest].run(testName, args)

  implicit val routeTestTimeout = RouteTestTimeout(5.seconds)

  // TODO: AgoraProjectionsSpec uses this but should not have to. Running those tests through ApiService exception handler fails the test for some reason.
  val wrapWithExceptionHandler = handleExceptions(ExceptionHandler {
    case e: IllegalArgumentException => complete(BadRequest, e.getMessage)
    case ve: ValidationException => complete(BadRequest, ve.getMessage)
  })

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService(permsDataSource) with ActorRefFactoryContext
  val configurationsService = new ConfigurationsService(permsDataSource) with ActorRefFactoryContext

  def uriEncode(uri: String): String = {
    java.net.URLEncoder.encode(uri, "UTF-8")
  }

  def brief(entities: Seq[AgoraEntity]): Seq[AgoraEntity] = {
    entities.map(entity =>
      AgoraEntity(namespace = entity.namespace,
        name = entity.name,
        snapshotId = entity.snapshotId,
        snapshotComment = entity.snapshotComment,
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
        snapshotComment = entity.snapshotComment,
        entityType = entity.entityType,
        url = Option(entity.agoraUrl)
      )
    )
  }
}

