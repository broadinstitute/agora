package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.ga4gh.Models.ToolDescriptorType.DescriptorType
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

object Ga4ghServiceMessages {

  case class QueryPublicSingle(entity: AgoraEntity)

  case class QueryPublicSinglePayload(entity: AgoraEntity,
                                      descriptorType: DescriptorType)

  case class QueryPublic(agoraSearch: AgoraEntity)

  case class QueryPublicTool(agoraSearch: AgoraEntity)

  case class QueryPublicTools()


}
