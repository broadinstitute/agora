package org.broadinstitute.dsde.agora.server.ga4gh

import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghServiceMessages._
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.broadinstitute.dsde.agora.server.webservice.PerRequestCreator
import spray.http.{MediaTypes, StatusCodes}
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.routing._

abstract class Ga4ghService(permissionsDataSource: PermissionsDataSource)
  extends HttpService with Ga4ghServiceSupport with PerRequestCreator with LazyLogging {

  override implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def queryHandler = Props(classOf[Ga4ghQueryHandler], permissionsDataSource, executionContext)

  def routes =
    pathPrefix("ga4gh" / "v1") {
      get {
        path("metadata") {
          respondWithMediaType(MediaTypes.`application/json`) {
            complete(StatusCodes.OK, Metadata())
          }
        } ~
        path("tool-classes") {
          val toolClassesResponse:Seq[ToolClass] = Seq(ToolClass.apply(Some(AgoraEntityType.Workflow)))
          complete(toolClassesResponse)
        } ~
        path("tools") { requestContext =>
          // TODO: query params and response headers
          val message = QueryPublicTools(requestContext)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment) { id => requestContext =>
          val entity = entityFromArguments(id)
          val message = QueryPublicTool(requestContext, entity)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions") { id => requestContext =>
          val entity = entityFromArguments(id)
          val message = QueryPublic(requestContext, entity)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions" / Segment) { (id, versionId) => requestContext =>
          val entity = entityFromArguments(id, versionId)
          val message = QueryPublicSingle(requestContext, entity)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions" / Segment / "dockerfile") { (id, versionId) =>
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor") { (id, versionId, descriptorType) => requestContext =>
          val entity = entityFromArguments(id, versionId)
          val message = QueryPublicSinglePayload(requestContext, entity, parseDescriptorType(descriptorType))
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor" / Segment) { (id, versionId, descriptorType, relativePath) =>
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "tests") { (id, versionId, descriptorType) =>
          complete(spray.http.StatusCodes.NotImplemented)
        }
      }
    }
}
