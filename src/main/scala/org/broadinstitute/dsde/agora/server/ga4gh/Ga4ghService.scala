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
    pathPrefix("ga4gh") {
      pathPrefix("tools") {
        path (Segment / "versions" / Segment / Segment / "descriptor") { (id, versionId, descriptorType) =>
          get { requestContext =>

            val dType = parseDescriptorType(descriptorType)
            val snapshotId = parseVersionId(versionId)
            val toolId = parseId(id)

            val entity = AgoraEntity(Some(toolId.namespace), Some(toolId.name), Some(snapshotId), entityType = Some(AgoraEntityType.Workflow))

            val message = QueryPublicSingle(requestContext, entity, dType)
            perRequest(requestContext, queryHandler, message)
          }
        }
      }
    }

}
