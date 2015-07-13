
package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.authorization.TestAuthorizationProvider._
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.broadinstitute.dsde.agora.server.webservice.ApiServiceSpec
import org.scalatest.DoNotDiscover

@DoNotDiscover
class AgoraAuthorizationTest extends ApiServiceSpec {
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

  "Agora" should "only authorize users with permissions in local permisison storage" in {
    val agoraBusiness = new AgoraBusiness()

    val agoraEntity = agoraBusiness.findSingle(testEntity1WithId.namespace.get,
                                          testEntity1WithId.name.get,
                                          testEntity1WithId.snapshotId.get,
                                          Seq(testEntity1WithId.entityType.get),
                                          agoraCIOwner.get)

    agoraEntity match {
      case Some(entity) => {

        //default permission is full authorization with testAuth
        assert(isAuthorizedForRead(entity, owner2.get) === true)
        assert(isAuthorizedForCreation(entity, owner2.get) === true)

        //Remove owner1's permissions
        addEntityPermission(hash(entity, owner1.get), AgoraPermissions(Nothing))
        addNamespacePermission(hash(entity, owner1.get), AgoraPermissions(Nothing))

        assert(isAuthorizedForRead(entity, owner1.get) === false)
        assert(isAuthorizedForCreation(entity, owner1.get) === false)

        //Add owner1's permissions
        createEntityAuthorizations(entity, owner1.get)

        assert(isAuthorizedForRead(entity, owner1.get) === true)
        assert(isAuthorizedForCreation(entity, owner1.get) === true)
      }
    }

  }

  "Agora" should "only return entities that can be read by user" in {

    val agoraBusiness = new AgoraBusiness()
    val entities = agoraBusiness.find(testEntity1WithId, None, Seq(AgoraEntityType.Workflow, AgoraEntityType.Task), agoraCIOwner.get)

    // Without agoraCIOwner's permissions
    addEntityPermission(hash(testEntity1WithId, agoraCIOwner.get), AgoraPermissions(Nothing))
    val noEntities = filterByReadPermissions(entities, agoraCIOwner.get)
    assert(noEntities.size === 0)
    assert(noEntities === Nil)

    // With agoraCIOwner's permissions
    createEntityAuthorizations(testEntity1WithId, agoraCIOwner.get)
    val oneEntity = filterByReadPermissions(entities, agoraCIOwner.get)
    assert(oneEntity.size === 1)
    assert(oneEntity === entities)
  }

}
