package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.AgoraTestData.{mockAuthenticatedOwner, _}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.EntityAccessControl
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import spray.testkit.{RouteTest, ScalatestRouteTest}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.duration._

@DoNotDiscover
class PermissionIntegrationSpec extends FlatSpec with RouteTest with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory: ActorSystem = system
  }

  val methodsService = new MethodsService(permsDataSource) with ActorRefFactoryContext

  var agoraEntity1: AgoraEntity = _
  var agoraEntity2: AgoraEntity = _
  var redactedEntity: AgoraEntity = _

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    agoraEntity1 = patiently(agoraBusiness.insert(testIntegrationEntity, mockAuthenticatedOwner.get))
    agoraEntity2 = patiently(agoraBusiness.insert(testIntegrationEntity2, owner2.get))
    redactedEntity = patiently(agoraBusiness.insert(testEntityToBeRedacted2, mockAuthenticatedOwner.get))
    patiently(agoraBusiness.delete(redactedEntity, Seq(redactedEntity.entityType.get), mockAuthenticatedOwner.get))
  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  "Agora" should "return namespace permissions. list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Manage")
    }
  }

  "Agora" should "not return namespace permissions. list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + "permissions") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == Forbidden)
      }
    }

  "Agora" should "allow authorized users to insert multiple roles in a single namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read,Create,Manage") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Create")
      }
  }

  "Agora" should "not allow authorized users to POST over their own namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == BadRequest)
      }
  }



  "Agora" should "not allow unauthorized users to insert a namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=All") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == Forbidden)
      }
  }

  "Agora" should "only allow authorized users to overwrite existing permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Read")
      }
  }

  "Agora" should "not allow authorized users to overwrite their own namespace permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "allow authorized users to delete an existing namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "[]")
      }
  }

  "Agora" should "not allow authorized users to delete their own namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "not allow unauthorized users to delete an existing namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=All") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == Forbidden)
      }
  }


  "Agora" should "return entity permissions. list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
        "/" + agoraEntity1.snapshotId.get + "/" + "permissions") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Manage")
      }
  }

  "Agora" should "not return entity permissions. list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == Forbidden)
      }
  }

  "Agora" should "allow authorized users to insert a entity permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Manage")
      }
  }

  "Agora" should "not allow authorized users to POST over their own entity permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "not allow unauthorized users to insert a entity permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions" + s"?user=$agoraTestOwner&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == Forbidden)
      }
  }

  "Agora" should "allow authorized users to edit an existing entity permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=Read") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Read")
      }
  }

  "Agora" should "not allow authorized users to edit their own entity permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "allow authorized users to delete an existing entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "[]")
      }
  }

  "Agora" should "not allow authorized users to delete their own entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=${mockAuthenticatedOwner.get}&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "not allow unauthorized users to delete an existing entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == Forbidden)
      }
  }

  "Agora" should "successfully list permissions for multiple methods simultaneously" in {

    val payload:Seq[AgoraEntity] = Seq(
      AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, agoraEntity1.snapshotId),
      AgoraEntity(agoraEntity2.namespace, agoraEntity2.name, agoraEntity2.snapshotId),
      AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, Some(12345)),
      AgoraEntity(redactedEntity.namespace, redactedEntity.name, redactedEntity.snapshotId)
    )

    Post(ApiUtil.Methods.withLeadingVersion + "/permissions", payload) ~>
      methodsService.multiEntityPermissionsRoute ~>
      check {
        assert(status == OK)
        val entityAclList = responseAs[Seq[EntityAccessControl]]
        assertResult(4) {entityAclList.size}

        // check first - should get permissions
        {
          val stubEntity = AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, agoraEntity1.snapshotId)
          val found = entityAclList.find(_.entity == stubEntity)
          assert(found.isDefined)
          assert(found.get.message.isEmpty)
          assert(found.get.acls.size == 1)
          assert(found.get.acls.head.user == mockAuthenticatedOwner.get)
          assert(found.get.acls.head.roles.canManage)
        }
        // check second - it exists, but we don't have permissions to see it
        {
          val stubEntity = AgoraEntity(agoraEntity2.namespace, agoraEntity2.name, agoraEntity2.snapshotId)
          val found = entityAclList.find(_.entity == stubEntity)
          assert(found.isDefined)
          assert(found.get.message.nonEmpty)
          assert(found.get.acls.isEmpty)
        }
        // check third - it doesn't exist in the db
        {
          val stubEntity = AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, Some(12345))
          val found = entityAclList.find(_.entity == stubEntity)
          assert(found.isDefined)
          assert(found.get.message.nonEmpty)
          assert(found.get.acls.isEmpty)
        }
        // check fourth - it has been redacted, which resolves to us not having permissions to see it
        {
          val stubEntity = AgoraEntity(redactedEntity.namespace, redactedEntity.name, redactedEntity.snapshotId)
          val found = entityAclList.find(_.entity == stubEntity)
          assert(found.isDefined)
          assert(found.get.message.nonEmpty)
          assert(found.get.acls.isEmpty)
        }
      }
  }

}
