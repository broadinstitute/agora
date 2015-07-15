package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraEntity}
import org.broadinstitute.dsde.agora.server.AgoraTestData._

object AgoraIntegrationTestData  {
  val testIntegrationEntity = AgoraEntity(namespace = Option("___test" + System.currentTimeMillis()),
                                          name = Option("testWorkflow"),
                                          synopsis = synopsis1,
                                          documentation = documentation1,
                                          owner = Option(AgoraConfig.gcsServiceAccountUserEmail),
                                          payload = payload1,
                                          entityType = Option(AgoraEntityType.Workflow))
}
