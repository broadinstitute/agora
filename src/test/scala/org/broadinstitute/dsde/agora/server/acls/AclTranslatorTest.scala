package org.broadinstitute.dsde.agora.server.acls

import org.scalatest.{Matchers, FlatSpec, DoNotDiscover}

/**
 * Created by dshiga on 6/17/15.
 */
@DoNotDiscover
class AclTranslatorTest extends FlatSpec with Matchers {

  "AclTranslator" should "translate GCS bucket READER -> Agora read" in {
    val bucketRole = GcsRole("READER")
    val acl = AclTranslator.gcsRolesToAgoraAcl(bucketRole, None)
    assert(acl.canRead === true)
    assert(acl.canWrite === false)
    assert(acl.canCreate === false)
    assert(acl.canRedact === false)
    assert(acl.canManage === false)
  }
  
  "AclTranslator" should "translate GCS object READER -> Agora read" in {
    val bucketRole = GcsRole("NONE")
    val objectRole = GcsRole("READER")
    val acl = AclTranslator.gcsRolesToAgoraAcl(bucketRole, Some(objectRole))
    assert(acl.canRead === true)
    assert(acl.canWrite === false)
    assert(acl.canCreate === false)
    assert(acl.canRedact === false)
    assert(acl.canManage === false)
  }

  "AclTranslator" should "translate GCS bucket WRITER -> Agora create, write, redact, read" in {
    val bucketRole = GcsRole("WRITER")
    val acl = AclTranslator.gcsRolesToAgoraAcl(bucketRole, None)
    assert(acl.canRead === true)
    assert(acl.canWrite === true)
    assert(acl.canCreate === true)
    assert(acl.canRedact === true)
    assert(acl.canManage === false)
  }

  "AclTranslator" should "translate GCS bucket OWNER -> Agora manage, create, write, redact, read" in {
    val bucketRole = GcsRole("OWNER")
    val acl = AclTranslator.gcsRolesToAgoraAcl(bucketRole, None)
    assert(acl.canRead === true)
    assert(acl.canWrite === true)
    assert(acl.canCreate === true)
    assert(acl.canRedact === true)
    assert(acl.canManage === true)
  }
}
