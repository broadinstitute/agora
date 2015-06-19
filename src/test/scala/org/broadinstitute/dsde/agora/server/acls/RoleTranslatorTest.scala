package org.broadinstitute.dsde.agora.server.acls

import org.scalatest.{Matchers, FlatSpec, DoNotDiscover}
import org.broadinstitute.dsde.agora.server.business.AgoraPermissions
import org.broadinstitute.dsde.agora.server.business.AgoraPermissions._

@DoNotDiscover
class RoleTranslatorTest extends FlatSpec with Matchers {

  "RoleTranslator" should "translate bucket Nothing -> namespace Nothing" in {
    val bucketRole = GcsBucketRole(GcsRole.Nothing)
    val namespacePermissions = RoleTranslator.gcsBucketToNamespacePermissions(bucketRole)
    assert(namespacePermissions.canRead === false)
    assert(namespacePermissions.canWrite === false)
    assert(namespacePermissions.canCreate === false)
    assert(namespacePermissions.canRedact === false)
    assert(namespacePermissions.canManage === false)
  }

  "RoleTranslator" should "translate bucket READER -> namespace read" in {
    val bucketRole = GcsBucketRole(GcsRole.Reader)
    val namespacePermissions = RoleTranslator.gcsBucketToNamespacePermissions(bucketRole)
    assert(namespacePermissions.canRead === true)
    assert(namespacePermissions.canCreate === false)
    assert(namespacePermissions.canManage === false)
  }

  "RoleTranslator" should "translate bucket WRITER -> namespace read, create" in {
    val bucketRole = GcsBucketRole(GcsRole.Writer)
    val namespacePermissions = RoleTranslator.gcsBucketToNamespacePermissions(bucketRole)
    assert(namespacePermissions.canRead === true)
    assert(namespacePermissions.canCreate === true)
    assert(namespacePermissions.canManage === false)
  }

  "RoleTranslator" should "translate bucket OWNER -> namespace read, create, manage" in {
    val bucketRole = GcsBucketRole(GcsRole.Owner)
    val namespacePermissions = RoleTranslator.gcsBucketToNamespacePermissions(bucketRole)
    assert(namespacePermissions.canRead === true)
    assert(namespacePermissions.canCreate === true)
    assert(namespacePermissions.canManage === true)
  }

  "RoleTranslator" should "translate object NOTHING -> no method permissions" in {
    val objectRole = GcsObjectRole(GcsRole.Nothing)
    val methodPermissions = RoleTranslator.gcsObjectToMethodPermissions(objectRole)
    assert(methodPermissions.canRead === false)
    assert(methodPermissions.canWrite === false)
    assert(methodPermissions.canRedact === false)
    assert(methodPermissions.canManage === false)
  }

  "RoleTranslator" should "translate object READER -> method read" in {
    val objectRole = GcsObjectRole(GcsRole.Reader)
    val methodPermissions = RoleTranslator.gcsObjectToMethodPermissions(objectRole)
    assert(methodPermissions.canRead === true)
    assert(methodPermissions.canWrite === false)
    assert(methodPermissions.canRedact === false)
    assert(methodPermissions.canManage === false)
  }

  "RoleTranslator" should "translate object OWNER -> method read, manage" in {
    val objectRole = GcsObjectRole(GcsRole.Owner)
    val methodPermissions = RoleTranslator.gcsObjectToMethodPermissions(objectRole)
    assert(methodPermissions.canRead === true)
    assert(methodPermissions.canWrite === false)
    assert(methodPermissions.canRedact === false)
    assert(methodPermissions.canManage === true)
  }

  "RoleTranslator" should "translate namespace Nothing -> bucket Nothing" in {
    val namespacePermissions = new AgoraPermissions(AgoraPermissions.Nothing)
    val bucketRole = RoleTranslator.namespaceToBucketRole(namespacePermissions)
    assert(bucketRole.isReader === false)
    assert(bucketRole.isWriter === false)
    assert(bucketRole.isOwner === false)
  }

  "RoleTranslator" should "translate namespace(read) -> bucket READER" in {
    val namespacePermissions = new AgoraPermissions(Read)
    val bucketRole = RoleTranslator.namespaceToBucketRole(namespacePermissions)
    assert(bucketRole.isReader === true)
    assert(bucketRole.isWriter === false)
    assert(bucketRole.isOwner === false)
  }

  "RoleTranslator" should "translate namespace(write) -> bucket WRITER" in {
    val namespacePermissions = new AgoraPermissions(Write)
    val bucketRole = RoleTranslator.namespaceToBucketRole(namespacePermissions)
    assert(bucketRole.isReader === true)
    assert(bucketRole.isWriter === true)
    assert(bucketRole.isOwner === false)
  }

  "RoleTranslator" should "translate namespace(manage) -> bucket OWNER" in {
    val namespacePermissions = new AgoraPermissions(Manage)
    val bucketRole = RoleTranslator.namespaceToBucketRole(namespacePermissions)
    assert(bucketRole.isReader === true)
    assert(bucketRole.isWriter === true)
    assert(bucketRole.isOwner === true)
  }

  "RoleTranslator" should "translate method Nothing -> object Nothing" in {
    val methodPermissions = new AgoraPermissions(Nothing)
    val objectRole = RoleTranslator.methodToObjectRole(methodPermissions)
    assert(objectRole.isReader === false)
    assert(objectRole.isOwner === false)
  }

  "RoleTranslator" should "translate method(read) -> object(READER)" in {
    val methodPermissions = new AgoraPermissions(Read)
    val objectRole = RoleTranslator.methodToObjectRole(methodPermissions)
    assert(objectRole.isReader === true)
    assert(objectRole.isOwner === false)
  }

  "RoleTranslator" should "translate method(manage) -> object(OWNER)" in {
    val methodPermissions = new AgoraPermissions(Manage)
    val objectRole = RoleTranslator.methodToObjectRole(methodPermissions)
    assert(objectRole.isReader === true)
    assert(objectRole.isOwner === true)
  }
}
