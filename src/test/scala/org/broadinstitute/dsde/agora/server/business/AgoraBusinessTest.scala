package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions.Read
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.exceptions.{NamespaceAuthorizationException, ValidationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec, Matchers}
import slick.dbio.DBIOAction

@DoNotDiscover
class AgoraBusinessTest extends FlatSpec with Matchers with BeforeAndAfterAll with AgoraTestFixture {

//  val methodImportResolver = new MethodImportResolver(agoraTestOwner.get, agoraBusiness)

  override protected def beforeAll() = {
    ensureDatabasesAreRunning()
    startMockWaas()

    setSingleMockWaasDescribeOkResponse(payload2DescribeResponse)
    patiently(agoraBusiness.insert(testEntityToBeRedacted3, mockAuthenticatedOwner.get, mockAccessToken))
  }

  override protected def afterAll() = {
    clearDatabases()
    stopMockWaas()
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
    setSingleMockWaasDescribeOkResponse(payload1DescribeResponse)

    val expected = patiently(agoraBusiness.insert(testAgoraEntityWithAllLegalNameChars, mockAuthenticatedOwner.get, mockAccessToken))
    val actual = patiently(agoraBusiness.findSingle(
      testAgoraEntityWithAllLegalNameChars,
      Seq(testAgoraEntityWithAllLegalNameChars.entityType.get),
      mockAuthenticatedOwner.get))

    assert(actual.name == expected.name)
    assert(actual.namespace == expected.namespace)
  }

  "Agora" should "throw an exception when inserting an entity with an illegal name" in {
    setSingleMockWaasDescribeOkResponse(payload1DescribeResponse)

    intercept[ValidationException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithIllegalNameChars, mockAuthenticatedOwner.get, mockAccessToken))
    }
  }

  "Agora" should "throw an exception when inserting an entity with an illegal namespace" in {
    intercept[ValidationException] {
      patiently(agoraBusiness.insert(testAgoraEntityWithIllegalNamespaceChars, mockAuthenticatedOwner.get, mockAccessToken))
    }
  }

  "Agora" should "allow a pre-existing entity with illegal name to be loaded successfully" in {

    implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
    val agoraDao = AgoraDao.createAgoraDao(Seq(testAgoraEntityWithIllegalNameChars.entityType.get))

    // Strategy: bypass the business rule validation in AgoraBusiness by inserting directly through the DAO & perms client
    // Once inserted, verify that AgoraBusiness.findSingle does not choke on the illegally-named entity

    // Insert entity
    agoraDao.insert(testAgoraEntityWithIllegalNameChars.addDate())

    // Set entity permissions
    patiently(permsDataSource.inTransaction { db =>
      DBIOAction.seq(
        db.aePerms.addEntity(testAgoraEntityWithIllegalNameChars),
        db.aePerms.insertEntityPermission(testAgoraEntityWithIllegalNameChars, new AccessControl(mockAuthenticatedOwner.get, AgoraPermissions(Read)))
      )
    })

    // Fetch single entity
    val actual = patiently(agoraBusiness.findSingle(
      testAgoraEntityWithIllegalNameChars,
      Seq(testAgoraEntityWithIllegalNameChars.entityType.get),
      mockAuthenticatedOwner.get))

    assert(actual.name == testAgoraEntityWithIllegalNameChars.name)
    assert(actual.namespace == testAgoraEntityWithIllegalNameChars.namespace)

    // Search for entity
    val actualFromSearch = patiently(
      agoraBusiness.find(testAgoraEntityWithIllegalNameChars, None, Seq(testAgoraEntityWithIllegalNameChars.entityType.get), mockAuthenticatedOwner.get)
    ).head

    assert(actualFromSearch.name == testAgoraEntityWithIllegalNameChars.name)
    assert(actualFromSearch.namespace == testAgoraEntityWithIllegalNameChars.namespace)
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
