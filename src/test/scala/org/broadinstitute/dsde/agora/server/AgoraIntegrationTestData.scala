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

  val testAgoraEntityWithValidOfficialDockerImageInWdl = new AgoraEntity(namespace = Option("___docker_test" + System.currentTimeMillis()),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(owner1),
    payload = payloadWithValidOfficialDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test" + System.currentTimeMillis()),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(owner1),
    payload = payloadWithInvalidOfficialDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidOfficialDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test" + System.currentTimeMillis()),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(owner1),
    payload = payloadWithInvalidOfficialDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithValidPersonalDockerInWdl = new AgoraEntity(namespace = Option("___docker_test" + System.currentTimeMillis()),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(owner1),
    payload = payloadWithValidPersonalDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerUserNameInWdl = new AgoraEntity(namespace = Option("___docker_test" + System.currentTimeMillis()),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(owner1),
    payload = payloadWithInvalidPersonalDockerUserNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test" + System.currentTimeMillis()),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(owner1),
    payload = payloadWithInvalidPersonalDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test" + System.currentTimeMillis()),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(owner1),
    payload = payloadWithInvalidPersonalDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )
}
