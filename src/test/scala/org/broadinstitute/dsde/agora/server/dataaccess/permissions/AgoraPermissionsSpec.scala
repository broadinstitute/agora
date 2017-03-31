
package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.{AgoraTestData, AgoraTestFixture}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.webservice.ApiServiceSpec
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover}

@DoNotDiscover
class AgoraPermissionsSpec extends ApiServiceSpec with BeforeAndAfterAll with AgoraTestFixture {

  override protected def beforeAll() = {
    ensureDatabasesAreRunning()
  }

  override protected def afterAll() = {
    clearDatabases()
  }

  "Agora" should "return true if someone has read access and we ask if they have read access " in {
    val authorization = AgoraPermissions(Read)
    assert(authorization.canRead)
  }

  "Agora" should "return true if someone has read and write access and we ask if they have read access " in {
    val authorization = AgoraPermissions(ReadWrite)
    assert(authorization.canRead)
  }

  "Agora" should "return false if someone has read access and we ask if they have write access " in {
    val authorization = AgoraPermissions(Read)
    assert(!authorization.canWrite)
  }

  "Agora" should "return true for all permissions if they have full access" in {
    val authorization = AgoraPermissions(All)
    assert(authorization.canRead)
    assert(authorization.canWrite)
    assert(authorization.canCreate)
    assert(authorization.canRedact)
    assert(authorization.canManage)
  }

  "Agora" should "be able to construct authorizations using var arg permissions" in {
    val authorization = new AgoraPermissions(Read, Write, Redact)
    assert(authorization.canRead)
    assert(authorization.canWrite)
    assert(authorization.canRedact)
    assert(!authorization.canCreate)
    assert(!authorization.canManage)
  }

  "Agora" should "be able to remove permissions from authorization using var arg permissions" in {
    val authorization = new AgoraPermissions(All)
    val newAuthorization = authorization.removePermissions(Write, Create)
    assert(newAuthorization.canRead)
    assert(!newAuthorization.canWrite)
    assert(newAuthorization.canRedact)
    assert(!newAuthorization.canCreate)
    assert(newAuthorization.canManage)
  }

  "Agora" should "be able to add permissions from an authorization using var arg permissions" in {
    val authorization = new AgoraPermissions(Read)
    val newAuthorization = authorization.addPermissions(Write, Create)
    assert(newAuthorization.canRead)
    assert(newAuthorization.canWrite)
    assert(!newAuthorization.canRedact)
    assert(newAuthorization.canCreate)
    assert(!newAuthorization.canManage)
  }

  "Agora" should "be able to list admins" in {
    addAdminUser()
    val adminUsers = runInDB { db => db.admPerms.listAdminUsers }
    assert(adminUsers.length == 1)
    assert(adminUsers.head == AgoraTestData.adminUser.get)
  }

  "Agora" should "be able to update the admin status of a user" in {
    val adminUser = AgoraTestData.adminUser.get

    // Set adminUsers's admin status false
    var noAdminUsers = runInDB { db =>
      db.admPerms.updateAdmin(adminUser, false) andThen
      db.admPerms.listAdminUsers
    }
    assert(noAdminUsers.isEmpty)

    // Set adminUsers's admin status true
    var someAdminUsers = runInDB { db =>
      db.admPerms.updateAdmin(adminUser, true) andThen
      db.admPerms.listAdminUsers
    }
    assert(someAdminUsers.length == 1)
  }
}
