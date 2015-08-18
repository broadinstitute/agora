package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraEntity}
import org.broadinstitute.dsde.agora.server.AgoraTestData._

object AgoraIntegrationTestData  {

  val agoraCIOwner = "agora-test"
  val mockAutheticatedOwner = AgoraConfig.mockAuthenticatedUserEmail
  val owner1 = "tester1"
  val owner2 = "tester2"

  val testIntegrationEntity = AgoraEntity(namespace = Option("___test1"),
                                          name = Option("testWorkflow"),
                                          synopsis = synopsis1,
                                          documentation = documentation1,
                                          owner = Option(owner1),
                                          payload = payload1,
                                          entityType = Option(AgoraEntityType.Workflow))

  val testIntegrationEntity2 = AgoraEntity(namespace = Option("___test2"),
                                            name = Option("testWorkflow"),
                                            synopsis = synopsis1,
                                            documentation = documentation1,
                                            owner = Option(owner2),
                                            payload = payload1,
                                            entityType = Option(AgoraEntityType.Workflow))

  val testAgoraEntityWithValidOfficialDockerImageInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(mockAutheticatedOwner),
    payload = payloadWithValidOfficialDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(mockAutheticatedOwner),
    payload = payloadWithInvalidOfficialDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidOfficialDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(mockAutheticatedOwner),
    payload = payloadWithInvalidOfficialDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithValidPersonalDockerInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(mockAutheticatedOwner),
    payload = payloadWithValidPersonalDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerUserNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(mockAutheticatedOwner),
    payload = payloadWithInvalidPersonalDockerUserNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(mockAutheticatedOwner),
    payload = payloadWithInvalidPersonalDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = Option(mockAutheticatedOwner),
    payload = payloadWithInvalidPersonalDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )
}
