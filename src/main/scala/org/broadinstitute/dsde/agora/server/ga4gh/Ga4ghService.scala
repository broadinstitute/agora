package org.broadinstitute.dsde.agora.server.ga4gh

import akka.actor.Props
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.PerRequestCreator
import org.broadinstitute.dsde.agora.server.webservice.handlers.QueryHandler
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages.QueryPublicSingle
import spray.routing._

abstract class Ga4ghService(permissionsDataSource: PermissionsDataSource)
  extends HttpService with Ga4ghServiceSupport with PerRequestCreator with LazyLogging {

  override implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def queryHandler = Props(classOf[QueryHandler], permissionsDataSource, executionContext)

  def routes =
    pathPrefix("ga4gh" / "v1") {
      get {
        path("metadata") {
          // TODO: metadata endpoint
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tool-classes") {
          // TODO: tool-classes endpoint
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools") {
            // TODO: tools endpoint
            complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment) { id =>
              // TODO: tools/{id} endpoint
              complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions") { id =>
          // TODO: tools/{id}/versions endpoint
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment) { (id, versionId) =>
          // TODO: tools/{id}/versions/{version-id} endpoint
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / "dockerfile") { (id, versionId) =>
          // TODO: tools/{id}/versions/{version-id}/dockerfile endpoint
          complete(spray.http.StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor") { (id, versionId, descriptorType) => requestContext =>
          // /tools/{id}/versions/{version-id}/{type}/descriptor endpoint

            val dType = parseDescriptorType(descriptorType)
            val snapshotId = parseVersionId(versionId)
            val toolId = parseId(id)

            val entity = AgoraEntity(Some(toolId.namespace), Some(toolId.name), Some(snapshotId), entityType = Some(AgoraEntityType.Workflow))

            val message = QueryPublicSingle(requestContext, entity, dType)
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
