package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.business.PermissionBusiness
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.NamespacePermissionsClient._
import org.broadinstitute.dsde.agora.server.exceptions.AgoraException
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}

@DoNotDiscover
class ManagerPermissionSpec extends FlatSpec with ScalaFutures with BeforeAndAfterAll with AgoraTestFixture {

  var agoraBusiness: AgoraBusiness = _
  var permissionBusiness: PermissionBusiness = _
  var testBatchPermissionEntityWithId: AgoraEntity = _

  val testEntity = AgoraEntity(namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = testEntity1.owner,
    payload = payload1,
    entityType = Option(AgoraEntityType.Task))


  override def beforeAll() = {
    ensureDatabasesAreRunning()
    agoraBusiness = new AgoraBusiness()
    permissionBusiness = new PermissionBusiness()
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "not delete the last manager namespace permission." in {
    agoraBusiness.insert(testEntity, mockAuthenticatedOwner.get)
    insertNamespacePermission(testEntity, AccessControl(adminUser.get, AgoraPermissions(All)))
    insertNamespacePermission(testEntity, AccessControl(owner1.get, AgoraPermissions(All)))
    insertNamespacePermission(testEntity, AccessControl(owner2.get, AgoraPermissions(Read)))
    insertNamespacePermission(testEntity, AccessControl(owner3.get, AgoraPermissions(ReadWrite)))

    deleteNamespacePermission(testEntity, mockAuthenticatedOwner.get)
    deleteNamespacePermission(testEntity, adminUser.get)
    val exception = intercept[AgoraException] {
      deleteNamespacePermission(testEntity, owner1.get)
    }
    assert(exception != null)
  }


}
