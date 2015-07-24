package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraEntity}
import org.broadinstitute.dsde.agora.server.AgoraTestData._

object AgoraIntegrationTestData  {
  val owner1 = "broadprometheustest@gmail.com"
  val owner2 = "jcarey@broadinstitute.org"

  val testIntegrationEntity = AgoraEntity(namespace = Option("___test" + System.currentTimeMillis()),
                                          name = Option("testWorkflow"),
                                          synopsis = synopsis1,
                                          documentation = documentation1,
                                          owner = Option(owner1),
                                          payload = payload1,
                                          entityType = Option(AgoraEntityType.Workflow))

  val testIntegrationEntity2 = AgoraEntity(namespace = Option("___test" + System.currentTimeMillis()),
                                            name = Option("testWorkflow"),
                                            synopsis = synopsis1,
                                            documentation = documentation1,
                                            owner = Option(owner2),
                                            payload = payload1,
                                            entityType = Option(AgoraEntityType.Workflow))



}
