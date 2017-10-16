package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.ga4gh.Models.ToolDescriptorType.DescriptorType
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import spray.routing.RequestContext

object Ga4ghServiceMessages {

  case class QueryPublicSingle(requestContext: RequestContext,
                               entity: AgoraEntity)

  case class QueryPublicSinglePayload(requestContext: RequestContext,
                                      entity: AgoraEntity,
                                      descriptorType: DescriptorType)

  case class QueryPublic(requestContext: RequestContext,
                         agoraSearch: AgoraEntity)

  case class QueryPublicTool(requestContext: RequestContext,
                             agoraSearch: AgoraEntity)

  case class QueryPublicTools(requestContext: RequestContext)


}
