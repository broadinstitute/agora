package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{UserDao, users}
import org.broadinstitute.dsde.agora.server.exceptions.{NamespaceAuthorizationException, ValidationException}
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

  "Agora" should "allow insertion of a method with all legal characters in its name and namespace" in {
    val expected = patiently(agoraBusiness.insert(testAgoraEntityWithAllLegalNameChars, mockAuthenticatedOwner.get))
    val actual = patiently(agoraBusiness.findSingle(
      testAgoraEntityWithAllLegalNameChars,
      Seq(testAgoraEntityWithAllLegalNameChars.entityType.get),
      mockAuthenticatedOwner.get))

    assert(actual.name == expected.name)
    assert(actual.namespace == expected.namespace)
  }

  "Agora" should "throw an exception when inserting an entity with an illegal name" in {
    intercept[ValidationException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithIllegalNameChars, mockAuthenticatedOwner.get))
    }
  }

  "Agora" should "throw an exception when inserting an entity with an illegal namespace" in {
    intercept[ValidationException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithIllegalNamespaceChars, mockAuthenticatedOwner.get))
    }
  }

  "Agora" should "not let users without permissions redact a method" in {
    val testEntityToBeRedactedWithId3 = patiently(agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAuthenticatedOwner.get)).head
    intercept[NamespaceAuthorizationException] {
      patiently(agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, owner2.get))
    }
  }

  "Agora" should "prevent admin users from redacting any method" in {
    addAdminUser()
    val testEntityToBeRedactedWithId3 = patiently(agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAuthenticatedOwner.get)).head
    intercept[NamespaceAuthorizationException] {
      patiently(agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, adminUser.get))
    }
  }

}
