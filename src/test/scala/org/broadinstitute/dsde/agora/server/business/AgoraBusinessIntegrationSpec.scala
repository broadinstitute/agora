package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class AgoraBusinessIntegrationSpec extends FlatSpec {
  val agoraBusiness = new AgoraBusiness()

  "Agora" should "be able to store a task configuration with a valid (extant) official docker image" in {
    val entity = agoraBusiness.insert(testAgoraEntityWithValidOfficialDockerImageInWdl, mockAutheticatedOwner)
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), mockAutheticatedOwner) === entity)
  }

  "Agora" should "be unable to store a task configuration with an invalid docker image (invalid/non-existent repo name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl, mockAutheticatedOwner)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid docker image (invalid/non-existent tag name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerTagNameInWdl, mockAutheticatedOwner)
    }
    assert(thrown != null)
  }

  "Agora" should "be able to store a task configuration with a valid (extant) personal docker image" in {
    val entity = agoraBusiness.insert(testAgoraEntityWithValidPersonalDockerInWdl, mockAutheticatedOwner)
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), mockAutheticatedOwner) === entity)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent user name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerUserNameInWdl, mockAutheticatedOwner)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent repo name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl, mockAutheticatedOwner)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent tag name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerTagNameInWdl, mockAutheticatedOwner)
    }
    assert(thrown != null)
  }
}
