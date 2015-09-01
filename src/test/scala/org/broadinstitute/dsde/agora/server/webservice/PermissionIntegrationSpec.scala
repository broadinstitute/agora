package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import spray.testkit.{ScalatestRouteTest, RouteTest}
import spray.http.StatusCodes._
import scala.concurrent.duration._

@DoNotDiscover
class PermissionIntegrationSpec extends FlatSpec with RouteTest with ScalatestRouteTest with BeforeAndAfterAll {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService() with ActorRefFactoryContext
  val agoraBusiness = new AgoraBusiness()

  var agoraEntity1: AgoraEntity = null
  var agoraEntity2: AgoraEntity = null

  override def beforeAll(): Unit = {
    agoraEntity1 = agoraBusiness.insert(testIntegrationEntity, mockAutheticatedOwner)
    agoraEntity2 = agoraBusiness.insert(testIntegrationEntity2, owner2)
  }

  "Agora" should "return namespace permissions. list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "permissions") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Manage")
    }
  }

  "Agora" should "not return namespace permissions. list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + "permissions") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == Unauthorized)
      }
    }

  "Agora" should "allow authorized users to insert mutiple roles in a single namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read,Create,Manage") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Create")
      }
  }

  "Agora" should "not allow unauthorized users to insert a namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=All") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

  "Agora" should "only allow authorized users to overwrite existing permissions." in {

    Put(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Read")
      }
  }

  "Agora" should "allow authorized users to delete an existing namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "[]")
      }
  }

  "Agora" should "not allow unauthorized users to delete an existing namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=All") ~>
      methodsService.namespacePermissionsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }


  "Agora" should "return entity permissions. list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
        "/" + agoraEntity1.snapshotId.get + "/" + "permissions") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Manage")
      }
  }

  "Agora" should "not return entity permissions. list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

  "Agora" should "allow authorized users to insert a entity permissions." in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Manage")
      }
  }

  "Agora" should "not allow unauthorized users to insert a entity permissions." in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions" + s"?user=$agoraCIOwner&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

  "Agora" should "allow authorized users to edit an existing entity permissions." in {

    Put(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=Read") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "Read")
      }
  }

  "Agora" should "allow authorized users to delete an existing entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString contains "[]")
      }
  }

  "Agora" should "not allow unauthorized users to delete an existing entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      methodsService.entityPermissionsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

}
