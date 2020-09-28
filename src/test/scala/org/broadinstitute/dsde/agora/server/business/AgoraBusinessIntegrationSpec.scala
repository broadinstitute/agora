package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover}

import scala.concurrent.ExecutionContext.Implicits.global

@DoNotDiscover
class AgoraBusinessIntegrationSpec extends AnyFlatSpec with BeforeAndAfterAll with AgoraTestFixture  {
  override protected def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    startMockWaas()
  }

  override protected def afterAll(): Unit = {
    clearDatabases()
    stopMockWaas()
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
