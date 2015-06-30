package org.broadinstitute.dsde.agora.server.webservice.util

import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import spray.routing.RequestContext

/**
 * Case classes representing messages to pass to service handler actors
 */
object ServiceMessages {

  case class QuerySingle(requestContext: RequestContext,
                         namespace: String,
                         name: String,
                         snapshotId: Int,
                         entityType: Seq[AgoraEntityType.EntityType],
                         onlyPayload: Boolean)

  case class Query(requestContext: RequestContext,
                   agoraSearch: AgoraEntity,
                   agoraProjection: Option[AgoraEntityProjection],
                   entityTypes: Seq[AgoraEntityType.EntityType])

  case class Add(requestContext: RequestContext,
                 agoraAddRequest: AgoraEntity)

}
