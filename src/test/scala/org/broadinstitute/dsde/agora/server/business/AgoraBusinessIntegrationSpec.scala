package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.{AgoraAkkaTest, AgoraTestFixture}
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}

@DoNotDiscover
class AgoraBusinessIntegrationSpec extends FlatSpec with BeforeAndAfterAll with AgoraTestFixture with AgoraAkkaTest {
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    ensureDatabasesAreRunning()
    startMockWaas()
  }

  override protected def afterAll(): Unit = {
    clearDatabases()
    stopMockWaas()
    super.afterAll()
  }

  "Agora" should "be able to store a task configuration with a valid (extant) official docker image" in {
    val entity = patiently(agoraBusiness.insert(testAgoraEntityWithValidOfficialDockerImageInWdl, mockAuthenticatedOwner.get, mockAccessToken)).addManagers(Seq(mockAuthenticatedOwner.get)).addIsPublic(false)
    val actual = patiently(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), mockAuthenticatedOwner.get))
    assert(actual == entity)
  }

  ignore should "be unable to store a task configuration with an invalid docker image (invalid/non-existent repo name)" in {
    intercept[DockerImageNotFoundException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl, mockAuthenticatedOwner.get, mockAccessToken))
    }
  }

  ignore should "be unable to store a task configuration with an invalid docker image (invalid/non-existent tag name)" in {
    intercept[DockerImageNotFoundException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithInvalidOfficialDockerTagNameInWdl, mockAuthenticatedOwner.get, mockAccessToken))
    }
  }

  "Agora" should "be able to store a task configuration with a valid (extant) personal docker image" in {
    val entity = patiently(agoraBusiness.insert(testAgoraEntityWithValidPersonalDockerInWdl, mockAuthenticatedOwner.get, mockAccessToken)).addManagers(Seq(mockAuthenticatedOwner.get)).addIsPublic(false)
    assert(patiently(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), mockAuthenticatedOwner.get)) == entity)
  }

  ignore should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent user name)" in {
    intercept[DockerImageNotFoundException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerUserNameInWdl, mockAuthenticatedOwner.get, mockAccessToken))
    }
  }

  ignore should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent repo name)" in {
    intercept[DockerImageNotFoundException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl, mockAuthenticatedOwner.get, mockAccessToken))
    }
  }

  ignore should "be unable to store a task configuration with an invalid personal docker image (invalid/non-existent tag name)" in {
    intercept[DockerImageNotFoundException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithInvalidPersonalDockerTagNameInWdl, mockAuthenticatedOwner.get, mockAccessToken))
    }
  }
}
