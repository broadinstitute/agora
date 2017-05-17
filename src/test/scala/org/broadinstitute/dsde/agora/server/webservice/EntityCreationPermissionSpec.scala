
package org.broadinstitute.dsde.agora.server.webservice

import java.util.UUID

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions.All
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import spray.http.StatusCodes._
import spray.http.StatusCode
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

@DoNotDiscover
class EntityCreationPermissionSpec extends ApiServiceSpec {

  var testEntity1WithId: AgoraEntity = _
  var testEntity2WithId: AgoraEntity = _
  var testEntity3WithId: AgoraEntity = _
  var testEntity4WithId: AgoraEntity = _
  var testEntity5WithId: AgoraEntity = _
  var testEntity6WithId: AgoraEntity = _
  var testEntity7WithId: AgoraEntity = _
  var testEntityToBeRedactedWithId: AgoraEntity = _

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
    // owner1 create in namespace2
    assertEntityRejection(owner1.get, namespace2.get, randUUID)
  }

  it should "allow creating a new snapshot of an existing entity if you own the entity" in {
    // owner1 create new snapshot of testEntity1WithId in namespace1
    assertEntityCreation(owner1.get, testEntity1WithId.namespace.get, testEntity1WithId.name.get)
  }

  it should "reject creating a new snapshot of an existing entity if you do not own the entity" in {
    // owner3 create new entity in namespace2 (success)
    assertEntityCreation(owner3.get, namespace2.get, randUUID)
    // owner3 create new snapshot of testEntity2WithId in namespace2 (fail)
    assertEntityRejection(owner3.get, testEntity2WithId.namespace.get, testEntity2WithId.name.get)
  }

  it should "DO SOMETHING WHEN creating a new snapshot of an existing entity if you own the entity BUT LACK PERMISSION ON THE NAMESPACE" in {
    val name = randUUID
    assertEntityCreation(owner1.get, namespace1.get, name)

    val ent = AgoraEntity(namespace1, Some(name), Some(1))

    // add owner2
    patiently(permissionBusiness.insertEntityPermission(
      ent, owner1.get, AccessControl(owner2.get, AgoraPermissions(All))
    ))
    // remove owner1
    patiently(permissionBusiness.deleteEntityPermission(
      ent, owner2.get, owner1.get
    ))
    val actualPermissions = patiently(permissionBusiness.listEntityPermissions(ent, owner2.get))
    assertResult( List(AccessControl(owner2.get, AgoraPermissions(All))), "should have modified snapshot permissions correctly" ) {
      actualPermissions
    }
    // at this point, the entity has one snapshot, owned by owner2, but owner2 does not have create permissions
    // in the namespace.
    assertEntityCreation(owner2.get, namespace1.get, name)
  }

  it should "allow creating a new snapshot of an existing entity if you own any previous snapshot of that entity" in {

    val name = randUUID
    assertEntityCreation(owner2.get, namespace2.get, name) // create snapshot 1
    assertEntityCreation(owner2.get, namespace2.get, name) // create snapshot 2

    val ent = AgoraEntity(namespace2, Some(name), Some(2))

    // add owner3 to snapshot 2
    patiently(permissionBusiness.insertEntityPermission(
      ent, owner2.get, AccessControl(owner3.get, AgoraPermissions(All))
    ))
    // remove owner2 from snapshot 2
    patiently(permissionBusiness.deleteEntityPermission(
      ent, owner3.get, owner2.get
    ))
    val actualPermissions = patiently(permissionBusiness.listEntityPermissions(ent, owner3.get))
    assertResult( List(AccessControl(owner3.get, AgoraPermissions(All))), "should have modified snapshot permissions correctly" ) {
      actualPermissions
    }
    // at this point, the  entity has two snapshots; first owned by owner2 and second owned by owner3.
    // therefore, either owner2 or owner3 should be able to create more snapshots.
    assertEntityCreation(owner3.get, namespace2.get, name)
    assertEntityCreation(owner2.get, namespace2.get, name)
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
      addHeader(MockAgoraDirectives.mockUserHeader, asUser) ~>
      methodsService.postRoute ~> check {
        assert(status == expectedStatus, response.message)
        if (expectedStatus == Created) {
          handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
            assert(entity.namespace == randomEntity.namespace)
            assert(entity.name == randomEntity.name)
            assert(entity.synopsis == randomEntity.synopsis)
            assert(entity.documentation == randomEntity.documentation)
            assert(entity.owner == randomEntity.owner)
            assert(entity.payload == randomEntity.payload)
            assert(entity.entityType == randomEntity.entityType)
            assert(entity.snapshotId.isDefined)
            assert(entity.createDate.isDefined)
          })
        }
    }
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
