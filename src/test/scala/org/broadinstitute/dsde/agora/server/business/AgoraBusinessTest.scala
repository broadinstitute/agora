package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.exceptions.NamespaceAuthorizationException
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class AgoraBusinessTest extends FlatSpec with Matchers with BeforeAndAfterAll with AgoraTestFixture {

  val agoraBusiness = new AgoraBusiness()

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    agoraBusiness.insert(testEntityToBeRedacted3, mockAuthenticatedOwner.get)
  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  "Agora" should "not let users without permissions redact a method" in {
    val testEntityToBeRedactedWithId3 = agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAuthenticatedOwner.get).head
    intercept[NamespaceAuthorizationException] {
      val rowsEdited: Int = agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, owner2.get)
    }
  }

  "Agora" should "allow admin users to redact any method" in {
    addAdminUser()
    val testEntityToBeRedactedWithId3 = agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAuthenticatedOwner.get).head
    val rowsEdited: Int = agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, adminUser.get)
    assert(rowsEdited === 1)
  }

}
