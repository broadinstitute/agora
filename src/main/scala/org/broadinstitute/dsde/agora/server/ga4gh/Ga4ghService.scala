package org.broadinstitute.dsde.agora.server.ga4gh

import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.broadinstitute.dsde.agora.server.webservice.PerRequestCreator
import org.broadinstitute.dsde.agora.server.webservice.handlers.QueryHandler
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
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
          // because of the dashes in these names, using the automatic spray json marshalling is annoying.
          // this is a constant response anyway, so hardcode it as a string.
          val metadataResponse:String =
            """
              |{
              |  "version": "1.0.0",
              |  "api-version": "1.0.0",
              |  "country": "USA",
              |  "friendly-name": "FireCloud"
              |}
            """.stripMargin
          respondWithMediaType(MediaTypes.`application/json`) {
            complete(StatusCodes.OK, metadataResponse)
          }
        } ~
        path("tool-classes") {
          val toolClassesResponse:Seq[ToolClass] = Seq(ToolClass.fromEntityType(Some(AgoraEntityType.Workflow)))
          complete(toolClassesResponse)
        } ~
        path("tools") { requestContext =>
          // tools endpoint
          // TODO: query params and response headers
          val message = QueryPublicTools(requestContext)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment) { id => requestContext =>
          // tools/{id} endpoint
          val entity = entityFromArguments(id)
          val message = QueryPublicTool(requestContext, entity)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions") { id => requestContext =>
          // tools/{id}/versions endpoint
          val entity = entityFromArguments(id)
          val message = QueryPublic(requestContext, entity)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions" / Segment) { (id, versionId) => requestContext =>
          // tools/{id}/versions/{version-id} endpoint
          val entity = entityFromArguments(id, versionId)
          val message = QueryPublicSingle(requestContext, entity)
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions" / Segment / "dockerfile") { (id, versionId) =>
          // TODO: tools/{id}/versions/{version-id}/dockerfile endpoint
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor") { (id, versionId, descriptorType) => requestContext =>
          // /tools/{id}/versions/{version-id}/{type}/descriptor endpoint
          val entity = entityFromArguments(id, versionId)
          val message = QueryPublicSinglePayload(requestContext, entity, parseDescriptorType(descriptorType))
          perRequest(requestContext, queryHandler, message)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor" / Segment) { (id, versionId, descriptorType, relativePath) =>
          // /tools/{id}/versions/{version-id}/{type}/descriptor/{relative-path} endpoint not supported
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "tests") { (id, versionId, descriptorType) =>
          // /tools/{id}/versions/{version-id}/{type}/tests endpoint not supported
          complete(spray.http.StatusCodes.NotImplemented)
        }
      }
    }
}
