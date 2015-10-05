package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}

@DoNotDiscover
class AgoraBusinessIntegrationSpec extends FlatSpec with BeforeAndAfterAll with AgoraTestFixture  {
  val agoraBusiness = new AgoraBusiness()

  override protected def beforeAll() = {
    ensureDatabasesAreRunning()
  }

  override protected def afterAll() = {
    clearDatabases()
  }

  "Agora" should "be able to store a task configuration with a valid (extant) official docker image" in {
    val entity = agoraBusiness.insert(testAgoraEntityWithValidOfficialDockerImageInWdl, mockAutheticatedOwner.get)
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), mockAutheticatedOwner.get) === entity)
  }

  "Agora" should "be unable to store a task configuration with an invalid docker image (invalid/non-existent repo name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl, mockAutheticatedOwner.get)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid docker image (invalid/non-existent tag name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerTagNameInWdl, mockAutheticatedOwner.get)
    }
    assert(thrown != null)
  }

  "Agora" should "be able to store a task configuration with a valid (extant) personal docker image" in {
    val entity = agoraBusiness.insert(testAgoraEntityWithValidPersonalDockerInWdl, mockAutheticatedOwner.get)
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), mockAutheticatedOwner.get) === entity)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent user name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerUserNameInWdl, mockAutheticatedOwner.get)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent repo name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl, mockAutheticatedOwner.get)
    }
    assert(thrown != null)
  }

  "Agora" should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent tag name)" in {
    val thrown = intercept[DockerImageNotFoundException] {
      agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerTagNameInWdl, mockAutheticatedOwner.get)
    }
    assert(thrown != null)
  }
}
