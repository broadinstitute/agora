package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{UserDao, users}
import org.broadinstitute.dsde.agora.server.exceptions.NamespaceAuthorizationException
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec, Matchers}

@DoNotDiscover
class AgoraBusinessTest extends FlatSpec with Matchers with BeforeAndAfterAll with AgoraTestFixture {

//  val methodImportResolver = new MethodImportResolver(agoraTestOwner.get, agoraBusiness)

  override protected def beforeAll() = {
    ensureDatabasesAreRunning()
    patiently(agoraBusiness.insert(testEntityToBeRedacted3, mockAuthenticatedOwner.get))
  }

  override protected def afterAll() = {
    clearDatabases()
  }

//  "Agora" should "not find a method payload when resolving a WDL import statement if the method has not been added" in {
//    val importString = "methods://broad.nonexistent.5400"
//    intercept[Exception] {
//      methodImportResolver.importResolver(importString)
//    }
//  }

//  "Agora" should "throw an exception when trying to resolve a WDL import that is improperly formatted" in {
//    val importString = "methods:broad.nonexistent.5400"
//    intercept[Exception] {
//      methodImportResolver.importResolver(importString)
//    }
//  }

  "Agora" should "not let users without permissions redact a method" in {
    val testEntityToBeRedactedWithId3 = patiently(agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAuthenticatedOwner.get)).head
    intercept[NamespaceAuthorizationException] {
      patiently(agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, owner2.get))
    }
  }

  "Agora" should "allow admin users to redact any method" in {
    addAdminUser()
    val testEntityToBeRedactedWithId3 = patiently(agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAuthenticatedOwner.get)).head
    val rowsEdited: Int = patiently(agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, adminUser.get))
    assert(rowsEdited == 1)
  }

}
