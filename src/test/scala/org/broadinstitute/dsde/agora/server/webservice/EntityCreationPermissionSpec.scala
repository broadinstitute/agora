
package org.broadinstitute.dsde.agora.server.webservice

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions.All
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

import org.scalatest.{DoNotDiscover, FlatSpecLike}

@DoNotDiscover
class EntityCreationPermissionSpec extends ApiServiceSpec with FlatSpecLike {

  var testEntity1WithId: AgoraEntity = _
  var testEntity2WithId: AgoraEntity = _
  var testEntity3WithId: AgoraEntity = _
  var testEntity4WithId: AgoraEntity = _
  var testEntity5WithId: AgoraEntity = _
  var testEntity6WithId: AgoraEntity = _
  var testEntity7WithId: AgoraEntity = _
  var testEntityToBeRedactedWithId: AgoraEntity = _

  val routes = ApiService.handleExceptionsAndRejections {
    methodsService.postRoute
  }

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    // create namespace1, owned by owner1; namespace creation is a side effect of entity creation
    testEntity1WithId = patiently(agoraBusiness.insert(testEntity1, owner1.get))
    // create namespace2, owned by owner2; namespace creation is a side effect of entity creation
    testEntity2WithId = patiently(agoraBusiness.insert(testEntity2, owner2.get))
    // add owner3 as owner on namespace2
    patiently(permissionBusiness.insertNamespacePermission(AgoraEntity(testEntity2.namespace), owner2.get, AccessControl(owner3.get, AgoraPermissions(All))))
  }

  override def afterAll() = {
    clearDatabases()
  }

  behavior of "Agora, when creating/editing entities (methods and configs)"

  it should "set up test fixtures correctly" in {
    val ns1perms = patiently(permissionBusiness.listNamespacePermissions(AgoraEntity(testEntity1.namespace), owner1.get))

    val expectedNs1Perms = List(AccessControl(owner1.get, AgoraPermissions(All)))

    assertResult(expectedNs1Perms) {ns1perms}

    val ns2perms = patiently(permissionBusiness.listNamespacePermissions(AgoraEntity(testEntity2.namespace), owner2.get))

    val expectedNs2Perms = List(
      AccessControl(owner2.get, AgoraPermissions(All)),
      AccessControl(owner3.get, AgoraPermissions(All))
    )

    assertResult(expectedNs2Perms) {ns2perms}
  }

  it should "allow creating a new entity if you have permissions on the namespace" in {
    // owner1 create in namespace1
    assertEntityCreation(owner1.get, namespace1.get, randUUID)
  }

  it should "reject creating a new entity if you lack permissions on the namespace" in {
    // owner1 has no perms on namespace2, though owner1 has created an entity in another namespace.
    assertEntityRejection(owner1.get, namespace2.get, randUUID)
    // owner3 has no perms on namespace, and owner3 has never created an entity.
    assertEntityRejection(owner3.get, namespace1.get, randUUID)
    // both the above cases should be exactly the same result, but testing slightly different permutations of fixtures.
  }

  it should "allow creating a new snapshot of an existing entity if you own the entity" in {
    // owner1 create new snapshot of testEntity1WithId in namespace1
    assertEntityCreation(owner1.get, testEntity1WithId.namespace.get, testEntity1WithId.name.get)
  }

  it should "reject creating a new snapshot of an existing entity if you do not own the entity" in {
    // owner3 create random new entity in namespace2: prove that owner3 has Create in the namespace
    assertEntityCreation(owner3.get, namespace2.get, randUUID)

    // assert that owner3 does not have any permissions on pre-existing testEntity2WithId
    val existingPerms = patiently(permissionBusiness.listEntityPermissions(testEntity2WithId, owner2.get))
    assert(!existingPerms.exists(_.user == owner3.get), "owner3 should not have any permissions on testEntity2WithId")

    // owner3 create new snapshot of testEntity2WithId in namespace2: should fail to create, because owner3 doesn't
    // have permissions on previous snapshots.
    assertEntityRejection(owner3.get, testEntity2WithId.namespace.get, testEntity2WithId.name.get)
  }

  it should "allow creating a new snapshot of an existing entity if you own a snapshot of the entity but lack permission on the namespace" in {
    val name = randUUID
    assertEntityCreation(owner1.get, namespace1.get, name)

    val ent = AgoraEntity(namespace1, Some(name), Some(1))

    // add owner2
    patiently(permissionBusiness.insertEntityPermission(
      ent, owner1.get, AccessControl(owner2.get, AgoraPermissions(All))
    ))
    // ensure owner2 was added and has permissions on the entity
    val actualPermissions = patiently(permissionBusiness.listEntityPermissions(ent, owner2.get))
    assertResult(Set(AccessControl(owner1.get, AgoraPermissions(All)), AccessControl(owner2.get, AgoraPermissions(All))), "should have modified snapshot permissions correctly" ) {
      actualPermissions.toSet
    }
    // ensure owner2 does NOT have permission on the namespace
    val actualNamespacePermissions = patiently(permissionBusiness.listNamespacePermissions(AgoraEntity(namespace1), owner1.get))
    assertResult(List(AccessControl(owner1.get, AgoraPermissions(All)))) {
      actualNamespacePermissions
    }
    // at this point, the entity has one snapshot, owned by owner2, but owner2 does not have create permissions
    // in the namespace. Owner2 should still be able to create a new snapshot of that entity.
    assertEntityCreation(owner2.get, namespace1.get, name)
  }

  it should "allow creating a new snapshot of an existing entity if you own any previous snapshot of that entity" in {

    val name = randUUID
    assertEntityCreation(owner2.get, namespace2.get, name) // create snapshot 1
    assertEntityCreation(owner2.get, namespace2.get, name) // create snapshot 2

    val snapshot2 = AgoraEntity(namespace2, Some(name), Some(2))

    // add owner3 to snapshot 2
    patiently(permissionBusiness.insertEntityPermission(
      snapshot2, owner2.get, AccessControl(owner3.get, AgoraPermissions(All))
    ))
    // remove owner2 from snapshot 2
    patiently(permissionBusiness.deleteEntityPermission(
      snapshot2, owner3.get, owner2.get
    ))
    val actualPermissions = patiently(permissionBusiness.listEntityPermissions(snapshot2, owner3.get))
    assertResult( List(AccessControl(owner3.get, AgoraPermissions(All))), "should have modified snapshot permissions correctly" ) {
      actualPermissions
    }
    // at this point, the  entity has two snapshots; first owned by owner2 and second owned by owner3.
    // therefore, either owner2 or owner3 should be able to create more snapshots.
    assertEntityCreation(owner3.get, namespace2.get, name)
    assertEntityCreation(owner2.get, namespace2.get, name)
  }

  it should "defer to namespace permissions if all previous snapshots are redacted" in {
    val entityName = randUUID

    // owner3 has no perms on namespace1 - expect rejection
    assertEntityRejection(owner3.get, namespace1.get, entityName)
    // owner1 create in namespace1 - expect success
    assertEntityCreation(owner1.get, namespace1.get, entityName)

    // now, redact the snapshot owner1 just created.
    redact(namespace1.get, entityName, 1, owner1.get)

    // having a redacted snapshot should make no difference for future creators,
    // and we should be able to repeat our original test cases.

    // owner3 has no perms on namespace1 - expect rejection
    assertEntityRejection(owner3.get, namespace1.get, entityName)
    // owner1 create in namespace1 - expect success
    assertEntityCreation(owner1.get, namespace1.get, entityName)

  }

  // =============================================================================================
  // support methods
  // =============================================================================================
  private def assertEntityCreation(asUser: String, namespace: String, name: String) =
    testEntityCreation(asUser, namespace, name, Created)

  private def assertEntityRejection(asUser: String, namespace: String, name: String) =
    testEntityCreation(asUser, namespace, name, Forbidden)

  private def testEntityCreation(asUser: String, namespace: String, name: String, expectedStatus: StatusCode) = {
    val randomEntity = createRandomMethod(namespace, name)

    Post(ApiUtil.Methods.withLeadingVersion, randomEntity) ~>
      addHeader(MockAgoraDirectives.mockAuthenticatedUserEmailHeader, asUser) ~>
      routes ~> check {
        assert(status == expectedStatus, response.toString)
        if (expectedStatus == Created) {
          val entity = responseAs[AgoraEntity]
          assert(entity.namespace == randomEntity.namespace)
          assert(entity.name == randomEntity.name)
          assert(entity.synopsis == randomEntity.synopsis)
          assert(entity.documentation == randomEntity.documentation)
          assert(entity.owner == randomEntity.owner)
          assert(entity.payload == randomEntity.payload)
          assert(entity.entityType == randomEntity.entityType)
          assert(entity.snapshotId.isDefined)
          assert(entity.createDate.isDefined)
        }
      }
  }

  private def redact(namespace:String, name:String, snapshotId: Int, caller: String) = {
    val ent = AgoraEntity(Some(namespace), Some(name), Some(snapshotId))
    patiently(agoraBusiness.delete(ent, Seq(AgoraEntityType.Workflow), caller))
  }

  private def createRandomMethod(namespace: String, name: String) = {
    AgoraEntity(
      namespace = Some(namespace),
      name = Some(name),
      synopsis = Some(randUUID),
      documentation = Some(randUUID),
      owner = Some(randUUID),
      payload = payload1,
      entityType = Some(AgoraEntityType.Workflow))
  }

  private def randUUID: String = UUID.randomUUID.toString




}
