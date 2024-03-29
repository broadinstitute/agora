package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.exceptions.PermissionModificationException
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover}

import scala.concurrent.ExecutionContext.Implicits.global

@DoNotDiscover
class NamespacePermissionsClientSpec extends AnyFlatSpec with ScalaFutures with BeforeAndAfterAll with AgoraTestFixture {

  var testBatchPermissionEntityWithId: AgoraEntity = _

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    startMockWaas()

    patiently(agoraBusiness.insert(testEntity1, mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testEntity2, mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testEntity3, mockAuthenticatedOwner.get, mockAccessToken))
    testBatchPermissionEntityWithId = patiently(agoraBusiness.find(testEntity3, None, Seq(testEntity3.entityType.get), mockAuthenticatedOwner.get)).head
  }

  override def afterAll(): Unit = {
    clearDatabases()
    stopMockWaas()
  }

  "Agora" should "add namespace permissions." in {
    val insertCount1 = runInDB { db =>
      db.nsPerms.insertNamespacePermission(testEntity1, AccessControl(testEntity1.owner.get, AgoraPermissions(All)))
    }
    val insertCount2 = runInDB { db =>
      db.nsPerms.insertNamespacePermission(testEntity2, AccessControl(testEntity2.owner.get, AgoraPermissions(All)))
    }
    assert(insertCount1 === 1)
    assert(insertCount2 === 1)
  }

  "Agora" should "should silently add a user to the db if not already there." in {
    runInDB { db =>
      db.nsPerms.insertNamespacePermission(testEntity1, AccessControl(owner3.get, AgoraPermissions(All)))
    }
  }

  "Agora" should "return namespace permissions for authorized users." in {
    val permissions = runInDB { db =>
      db.nsPerms.getNamespacePermission(testEntity1, testEntity1.owner.get)
    }
    assert(permissions.canManage)
    assert(permissions.canCreate)
    assert(permissions.canRead)
    assert(permissions.canRedact)
    assert(permissions.canWrite)
  }

  "Agora" should "allow creation of namespaces that do not exist" in {
    val testEntityNewNamespace = testEntity1.copy(namespace = Option("unused_namepace2"))
    val permissions = runInDB { db =>
      db.nsPerms.getNamespacePermission(testEntityNewNamespace, testEntity2.owner.get)
    }
    assert(permissions.canCreate)
    assert(!permissions.canManage)
    assert(!permissions.canRead)
    assert(!permissions.canRedact)
    assert(!permissions.canWrite)
  }

  "Agora" should "give no permissions when namespace exists and user is unauthorized" in {
    val permissions = runInDB { db =>
      db.nsPerms.getNamespacePermission(testEntity2, testEntity1.owner.get)
    }
    assert(!permissions.canCreate)
    assert(!permissions.canManage)
    assert(!permissions.canRead)
    assert(!permissions.canRedact)
    assert(!permissions.canWrite)
  }

  "Agora" should "edit namespace permissions" in {
    val editCount = runInDB { db =>
      db.nsPerms.editNamespacePermission(testEntity2, AccessControl(testEntity2.owner.get, AgoraPermissions(Redact)))
    }
    assert(editCount == 1)

    val permissions = runInDB { db =>
      db.nsPerms.getNamespacePermission(testEntity2, testEntity2.owner.get)
    }
    assert(permissions.canRedact)
    assert(!permissions.canManage)
    assert(!permissions.canCreate)
    assert(!permissions.canRead)
    assert(!permissions.canWrite)
  }

  "Agora" should "delete namespace permissions" in {
    val deleteCount = runInDB { db =>
      db.nsPerms.deleteNamespacePermission(testEntity2, testEntity2.owner.get)
    }
    assert(deleteCount == 1)
  }

  "Agora" should "allow batch permission edits" in {
    val accessObject1 = AccessControl(owner1.get, AgoraPermissions(AgoraPermissions.All))
    val accessObject2 = AccessControl(owner2.get, AgoraPermissions(AgoraPermissions.Nothing))
    val rowsEditted = patiently(permissionBusiness.batchNamespacePermission(testBatchPermissionEntityWithId, mockAuthenticatedOwner.get, List(accessObject1, accessObject2)))
    assert(rowsEditted == 2)
  }

  "Agora" should "prevent a user from overwriting their own namespace permission" in {
    val accessObject = AccessControl(owner1.get, AgoraPermissions(AgoraPermissions.Nothing))
    intercept[PermissionModificationException] {
      patiently(permissionBusiness.insertNamespacePermission(testEntity1, owner1.get, accessObject))
    }
  }

  "Agora" should "prevent a user from modifying their own namespace permission" in {
    val accessObject = AccessControl(owner1.get, AgoraPermissions(AgoraPermissions.Nothing))
    intercept[PermissionModificationException] {
      patiently(permissionBusiness.editNamespacePermission(testEntity1, owner1.get, accessObject))
    }
  }

  "Agora" should "prevent a user from deleting their own namespace permission" in {
    intercept[PermissionModificationException] {
      patiently(permissionBusiness.deleteNamespacePermission(testEntity1, owner1.get, owner1.get))
    }
  }

}
