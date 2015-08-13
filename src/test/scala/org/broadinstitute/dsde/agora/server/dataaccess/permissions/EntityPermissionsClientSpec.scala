package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient._
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import scala.concurrent.duration._

class EntityPermissionsClientSpec extends FlatSpec with ScalaFutures with BeforeAndAfterAll {

  var foundTestEntity1: AgoraEntity = _
  var foundTestEntity2: AgoraEntity = _

  override def beforeAll() = {
    val agoraBusiness = new AgoraBusiness()
    foundTestEntity1 = agoraBusiness.find(testEntity1, None, Seq(testEntity1.entityType.get), mockAutheticatedOwner.get).head;
    foundTestEntity2 = agoraBusiness.find(testEntity2, None, Seq(testEntity2.entityType.get), mockAutheticatedOwner.get).head;
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
    val newEntity = foundTestEntity1.copy(snapshotId=Option(1234))
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

}
