package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.busines.PermissionBusiness
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{DoNotDiscover, BeforeAndAfterAll, FlatSpec}

@DoNotDiscover
class EntityPermissionsClientSpec extends FlatSpec with ScalaFutures with BeforeAndAfterAll with AgoraTestFixture {

  var agoraBusiness: AgoraBusiness = _
  var permissionBusiness: PermissionBusiness = _
  var foundTestEntity1: AgoraEntity = _
  var foundTestEntity2: AgoraEntity = _
  var testEntityWithPublicPermissionsWithId: AgoraEntity = _
  var testBatchPermissionEntity: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    agoraBusiness = new AgoraBusiness()
    permissionBusiness = new PermissionBusiness()
    agoraBusiness.insert(testEntity1, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntityWithPublicPermissions, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity3, mockAutheticatedOwner.get)
    agoraBusiness.insert(testEntity4, mockAutheticatedOwner.get)
    testEntityWithPublicPermissionsWithId = agoraBusiness.find(testEntityWithPublicPermissions, None, Seq(testEntityWithPublicPermissions.entityType.get), mockAutheticatedOwner.get).head;
    foundTestEntity1 = agoraBusiness.find(testEntity1, None, Seq(testEntity1.entityType.get), mockAutheticatedOwner.get).head
    foundTestEntity2 = agoraBusiness.find(testEntity2, None, Seq(testEntity2.entityType.get), mockAutheticatedOwner.get).head
    testBatchPermissionEntity = agoraBusiness.find(testEntity4, None, Seq(testEntity3.entityType.get), mockAutheticatedOwner.get).head
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "add entity permissions." in {
    val insertCount1 = insertEntityPermission(foundTestEntity1, AccessControl(foundTestEntity1.owner.get, AgoraPermissions(All)))
    val insertCount2 = insertEntityPermission(foundTestEntity2, AccessControl(foundTestEntity2.owner.get, AgoraPermissions(All)))
    assert(insertCount1 == 1)
    assert(insertCount2 == 1)
  }

  "Agora" should "should silently add a user to the db if not already there." in {
    insertEntityPermission(foundTestEntity1, AccessControl(owner3.get, AgoraPermissions(All)))
  }

  "Agora" should "return entity permissions for authorized users." in {
    val permissions = getEntityPermission(foundTestEntity1, foundTestEntity1.owner.get)
    assert(permissions.canManage)
    assert(permissions.canCreate)
    assert(permissions.canRead)
    assert(permissions.canRedact)
    assert(permissions.canWrite)
  }

  "Agora" should "can create in namespaces that do not exist" in {
    val testEntityNewNamespace = foundTestEntity1.copy(namespace = Option("unused_namespace"))
    val permissions = getEntityPermission(testEntityNewNamespace, foundTestEntity2.owner.get)
    assert(permissions.canCreate)
    assert(!permissions.canManage)
    assert(!permissions.canRead)
    assert(!permissions.canRedact)
    assert(!permissions.canWrite)
  }

  "Agora" should "reject entity permissions for unauthorized users if entity already exists." in {
    val permissions = getEntityPermission(foundTestEntity1, foundTestEntity2.owner.get)
    assert(!permissions.canManage)
    assert(!permissions.canCreate)
    assert(!permissions.canRead)
    assert(!permissions.canRedact)
    assert(!permissions.canWrite)
  }

  "Agora" should "allow users to create entities that do not exist" in {
    val newEntity = foundTestEntity1.copy(snapshotId = Option(1234))
    val permissions = getEntityPermission(newEntity, foundTestEntity2.owner.get)
    assert(permissions.canCreate)
    assert(!permissions.canManage)
    assert(!permissions.canRead)
    assert(!permissions.canRedact)
    assert(!permissions.canWrite)
  }

  "Agora" should "edit entity permissions" in {
    val user2 = foundTestEntity2.owner.get
    val editCount = editEntityPermission(foundTestEntity2, AccessControl(user2, AgoraPermissions(Redact)))
    assert(editCount == 1)

    val permissions = getEntityPermission(foundTestEntity2, user2)
    assert(permissions.canRedact)
    assert(!permissions.canManage)
    assert(!permissions.canCreate)
    assert(!permissions.canRead)
    assert(!permissions.canWrite)
  }

  "Agora" should "delete entity permissions" in {
    val deleteCount = deleteEntityPermission(foundTestEntity2, foundTestEntity2.owner.get)
    assert(deleteCount == 1)
  }

  "Agora" should "be able to insert a 'public' permission" in {
    val insertCount = insertEntityPermission(testEntityWithPublicPermissionsWithId, AccessControl("public", AgoraPermissions(All)))
    assert(insertCount == 1)
  }

  "Agora" should "allow public methods to be accessible" in {
    agoraBusiness.findSingle(testEntityWithPublicPermissionsWithId, Seq(testEntityWithPublicPermissions.entityType.get), owner2.get)
    assert(1 === 1) //test passes as long as findSingle does not throw permissions error
  }

  "Agora" should "allow batch permission edits" in {
    val accessObject1 = new AccessControl(owner1.get, AgoraPermissions(AgoraPermissions.All))
    val accessObject2 = new AccessControl(owner2.get, AgoraPermissions(AgoraPermissions.Nothing))
    val rowsEditted = permissionBusiness.batchEntityPermission(testBatchPermissionEntity, mockAutheticatedOwner.get, List(accessObject1, accessObject2))
    assert(rowsEditted === 2)
  }

}