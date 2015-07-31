package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsAuthorizationProvider
import org.broadinstitute.dsde.agora.server.webservice.util.DockerHubClient.DockerImageNotFoundException
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class AgoraBusinessIntegrationSpec extends FlatSpec {
  val agoraBusiness = new AgoraBusiness(GcsAuthorizationProvider)

  "Agora" should "be able to store a task configuration with a valid (extant) official docker image" in {
    val entity = agoraBusiness.insert(testAgoraEntityWithValidOfficialDockerImageInWdl, owner1)
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), owner1) === entity)
  }

  "Agora" should "be unable to store a task configuration with an invalid docker image (invalid/non-existent repo name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl, owner1)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid docker image (invalid/non-existent tag name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerTagNameInWdl, owner1)
    }
    assert(thrown != null)
  }

  "Agora" should "be able to store a task configuration with a valid (extant) personal docker image" in {
    val entity = agoraBusiness.insert(testAgoraEntityWithValidPersonalDockerInWdl, owner1)
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), owner1) === entity)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent user name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerUserNameInWdl, owner1)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent repo name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl, owner1)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent tag name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerTagNameInWdl, owner1)
    }
    assert(thrown != null)
  }
}
