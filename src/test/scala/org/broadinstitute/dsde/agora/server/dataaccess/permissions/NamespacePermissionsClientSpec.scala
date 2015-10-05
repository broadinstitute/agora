package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.busines.PermissionBusiness
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.NamespacePermissionsClient._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{DoNotDiscover, BeforeAndAfterAll, FlatSpec}

@DoNotDiscover
class NamespacePermissionsClientSpec extends FlatSpec with ScalaFutures with BeforeAndAfterAll with AgoraTestFixture {

  var agoraBusiness: AgoraBusiness = _
  var permissionBusiness: PermissionBusiness = _
  var testBatchPermissionEntityWithId: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    agoraBusiness = new AgoraBusiness()
    permissionBusiness = new PermissionBusiness()
    agoraBusiness.insert(testEntity1, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity3, mockAutheticatedOwner.get)
    testBatchPermissionEntityWithId = agoraBusiness.find(testEntity3, None, Seq(testEntity3.entityType.get), mockAutheticatedOwner.get).head
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "add namespace permissions." in {
    val insertCount1 = insertNamespacePermission(testEntity1, AccessControl(testEntity1.owner.get, AgoraPermissions(All)))
    val insertCount2 = insertNamespacePermission(testEntity2, AccessControl(testEntity2.owner.get, AgoraPermissions(All)))
    assert(insertCount1 === 1)
    assert(insertCount2 === 1)
  }

  "Agora" should "should silently add a user to the db if not already there." in {
    insertNamespacePermission(testEntity1, AccessControl(owner3.get, AgoraPermissions(All)))
  }

  "Agora" should "return namespace permissions for authorized users." in {
    val permissions = getNamespacePermission(testEntity1, testEntity1.owner.get)
    assert(permissions.canManage)
    assert(permissions.canCreate)
    assert(permissions.canRead)
    assert(permissions.canRedact)
    assert(permissions.canWrite)
  }

  "Agora" should "allow creation of namespaces that do not exist" in {
    val testEntityNewNamespace = testEntity1.copy(namespace = Option("unused_namepace2"))
    val permissions = getNamespacePermission(testEntityNewNamespace, testEntity2.owner.get)
    assert(permissions.canCreate)
    assert(!permissions.canManage)
    assert(!permissions.canRead)
    assert(!permissions.canRedact)
    assert(!permissions.canWrite)
  }

  "Agora" should "give no permissions when namespace exists and user is unauthorized" in {
    val permissions = getNamespacePermission(testEntity2, testEntity1.owner.get)
    assert(!permissions.canCreate)
    assert(!permissions.canManage)
    assert(!permissions.canRead)
    assert(!permissions.canRedact)
    assert(!permissions.canWrite)
  }

  "Agora" should "edit namespace permissions" in {
    val editCount = editNamespacePermission(testEntity2, AccessControl(testEntity2.owner.get, AgoraPermissions(Redact)))
    assert(editCount == 1)

    val permissions = getNamespacePermission(testEntity2, testEntity2.owner.get)
    assert(permissions.canRedact)
    assert(!permissions.canManage)
    assert(!permissions.canCreate)
    assert(!permissions.canRead)
    assert(!permissions.canWrite)
  }

  "Agora" should "delete namespace permissions" in {
    val deleteCount = deleteNamespacePermission(testEntity2, testEntity2.owner.get)
    assert(deleteCount == 1)
  }

  "Agora" should "allow batch permission edits" in {
    val accessObject1 = new AccessControl(owner1.get, AgoraPermissions(AgoraPermissions.All))
    val accessObject2 = new AccessControl(owner2.get, AgoraPermissions(AgoraPermissions.Nothing))
    val rowsEditted = permissionBusiness.batchNamespacePermission(testBatchPermissionEntityWithId, mockAutheticatedOwner.get, List(accessObject1, accessObject2))
    assert(rowsEditted === 2)
  }
}
