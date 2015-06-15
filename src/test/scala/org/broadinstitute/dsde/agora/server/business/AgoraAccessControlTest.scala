
package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.business.AgoraPermissions._
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}

@DoNotDiscover
class AgoraAccessControlTest extends FlatSpec with Matchers {
  "Agora" should "return true if someone has read access and we ask if they have read access " in {
    val acl = AgoraAcl(Read)
    assert(acl.canRead === true)
  }

  "Agora" should "return true if someone has read and write access and we ask if they have read access " in {
    val acl = AgoraAcl(ReadWrite)
    assert(acl.canRead === true)
  }

  "Agora" should "return false if someone has read access and we ask if they have write access " in {
    val acl = AgoraAcl(Read)
    assert(acl.canWrite === false)
  }

  "Agora" should "return true for all permissions if they have full access" in {
    val acl = AgoraAcl(All)
    assert(acl.canRead === true)
    assert(acl.canWrite === true)
    assert(acl.canCreate === true)
    assert(acl.canRedact === true)
    assert(acl.canManage === true)
  }

  "Agora" should "be able to construct an ACL using var arg permissions" in {
    val acl = new AgoraAcl(Read, Write, Redact)
    assert(acl.canRead === true)
    assert(acl.canWrite === true)
    assert(acl.canRedact === true)
    assert(acl.canCreate === false)
    assert(acl.canManage === false)
  }

  "Agora" should "be able to remove permissions from an ACL using var arg permissions" in {
    val acl = new AgoraAcl(All)
    val newAcl = acl.removePermissions(Write, Create)
    assert(newAcl.canRead === true)
    assert(newAcl.canWrite === false)
    assert(newAcl.canRedact === true)
    assert(newAcl.canCreate === false)
    assert(newAcl.canManage === true)
  }

  "Agora" should "be able to add permissions from an ACL using var arg permissions" in {
    val acl = new AgoraAcl(Read)
    val newAcl = acl.addPermissions(Write, Create)
    assert(newAcl.canRead === true)
    assert(newAcl.canWrite === true)
    assert(newAcl.canRedact === false)
    assert(newAcl.canCreate === true)
    assert(newAcl.canManage === false)
  }
}
