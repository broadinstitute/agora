
package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.AgoraTestFixture
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
    assert(authorization.canRead === true)
  }

  "Agora" should "return true if someone has read and write access and we ask if they have read access " in {
    val authorization = AgoraPermissions(ReadWrite)
    assert(authorization.canRead === true)
  }

  "Agora" should "return false if someone has read access and we ask if they have write access " in {
    val authorization = AgoraPermissions(Read)
    assert(authorization.canWrite === false)
  }

  "Agora" should "return true for all permissions if they have full access" in {
    val authorization = AgoraPermissions(All)
    assert(authorization.canRead === true)
    assert(authorization.canWrite === true)
    assert(authorization.canCreate === true)
    assert(authorization.canRedact === true)
    assert(authorization.canManage === true)
  }

  "Agora" should "be able to construct authorizations using var arg permissions" in {
    val authorization = new AgoraPermissions(Read, Write, Redact)
    assert(authorization.canRead === true)
    assert(authorization.canWrite === true)
    assert(authorization.canRedact === true)
    assert(authorization.canCreate === false)
    assert(authorization.canManage === false)
  }

  "Agora" should "be able to remove permissions from authorization using var arg permissions" in {
    val authorization = new AgoraPermissions(All)
    val newAuthorization = authorization.removePermissions(Write, Create)
    assert(newAuthorization.canRead === true)
    assert(newAuthorization.canWrite === false)
    assert(newAuthorization.canRedact === true)
    assert(newAuthorization.canCreate === false)
    assert(newAuthorization.canManage === true)
  }

  "Agora" should "be able to add permissions from an authorization using var arg permissions" in {
    val authorization = new AgoraPermissions(Read)
    val newAuthorization = authorization.addPermissions(Write, Create)
    assert(newAuthorization.canRead === true)
    assert(newAuthorization.canWrite === true)
    assert(newAuthorization.canRedact === false)
    assert(newAuthorization.canCreate === true)
    assert(newAuthorization.canManage === false)
  }
}
