package org.broadinstitute.dsde.agora.server.ga4gh

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghServiceMessages._
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

class Ga4ghService(permissionsDataSource: PermissionsDataSource) extends Ga4ghServiceSupport with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  implicit val executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout: akka.util.Timeout = 120.seconds
  val system = ActorSystem("Ga4ghServiceSystem")
  def queryHandler = Props(classOf[Ga4ghQueryHandler], permissionsDataSource, executionContext)
  val ga4ghActor: ActorRef = system.actorOf(queryHandler, "Ga4ghServiceActor_" + System.nanoTime())

  def divide(a: Int, b: Int): Future[Int] = Future {
    a / b
  }

  def routes: Route =
    pathPrefix("ga4gh" / "v1") {
      get {
        path("metadata") {
          complete(StatusCodes.OK, Metadata())
        } ~
        path("tool-classes") {
          val toolClassesResponse:Seq[ToolClass] = Seq(ToolClass.apply(Some(AgoraEntityType.Workflow)))
          complete(toolClassesResponse)
        } ~
        path("tools") {
          // TODO: query params and response headers
          val message = QueryPublicTools()
          val t: Future[Seq[Tool]] = (ga4ghActor ? message).mapTo[Seq[Tool]]
          completeOrRecoverWith(t) { failure => failWith(failure) }
        } ~
        path("tools" / Segment) { id =>
          val entity = entityFromArguments(id)
          val message = QueryPublicTool(entity)
          val t: Future[Tool] = (ga4ghActor ? message).mapTo[Tool]
          completeOrRecoverWith(t) { failure => failWith(failure) }
        } ~
        path("tools" / Segment / "versions") { id =>
          val entity = entityFromArguments(id)
          val message = QueryPublic(entity)
          val t: Future[Seq[ToolVersion]] = (ga4ghActor ? message).mapTo[Seq[ToolVersion]]
          completeOrRecoverWith(t) { failure => failWith(failure) }
        } ~
        path("tools" / Segment / "versions" / Segment) { (id, versionId) =>
          val entity = entityFromArguments(id, versionId)
          val message = QueryPublicSingle(entity)
          val t: Future[ToolVersion] = (ga4ghActor ? message).mapTo[ToolVersion]
          completeOrRecoverWith(t) { failure => failWith(failure) }
        } ~
        path("tools" / Segment / "versions" / Segment / "dockerfile") { (id, versionId) =>
          complete(StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor") { (id, versionId, descriptorType) =>
          val entity = entityFromArguments(id, versionId)
          val message = QueryPublicSinglePayload(entity, parseDescriptorType(descriptorType))
          val t: Future[JsValue] = (ga4ghActor ? message).mapTo[JsValue]
          completeOrRecoverWith(t) { failure => failWith(failure) }
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor" / Segment) { (id, versionId, descriptorType, relativePath) =>
          complete(StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "tests") { (id, versionId, descriptorType) =>
          complete(StatusCodes.NotImplemented)
        }
      }
    }
}
