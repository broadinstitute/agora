package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.{GcsAuthorizationProvider}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import spray.testkit.{ScalatestRouteTest, RouteTest}
import spray.http.StatusCodes._
import scala.concurrent.duration._

@DoNotDiscover
class AclIntegrationSpec extends FlatSpec with RouteTest with ScalatestRouteTest with BeforeAndAfterAll {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val methodsService = new MethodsService(GcsAuthorizationProvider) with ActorRefFactoryContext with AgoraOpenAMMockDirectives
  val agoraBusiness = new AgoraBusiness(GcsAuthorizationProvider)

  var agoraEntity1: AgoraEntity = null
  var agoraEntity2: AgoraEntity = null

  override def beforeAll(): Unit = {
    agoraEntity1 = agoraBusiness.insert(testIntegrationEntity, owner1)
    agoraEntity2 = agoraBusiness.insert(testIntegrationEntity2, owner2)
  }

  "Agora" should "return namespace ACL list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "acls") ~>
      methodsService.namespaceAclsRoute ~>
      check {
        assert(status == OK)
    }
  }

  "Agora" should " not return namespace ACL list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + "acls") ~>
      methodsService.namespaceAclsRoute ~>
      check {
        assert(status == Unauthorized)
      }
    }

  "Agora" should "allow authorized users to insert a namespace ACL" in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "acls" +
      s"?user=$owner2&role=OWNER") ~>
      methodsService.namespaceAclsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString.contains(owner2))
      }
  }

  "Agora" should "not allow unauthorized users to insert a namespace ACL" in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + "acls" +
      s"?user=$owner2&role=OWNER") ~>
      methodsService.namespaceAclsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

  "Agora" should "only allow authorized users to overwrite existing ACLS" in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "acls" +
      s"?user=$owner2&role=READER") ~>
      methodsService.namespaceAclsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString.contains("READER"))
      }
  }

  "Agora" should "allow authorized users to delete an existing namespace ACL" in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + "acls" +
      s"?user=$owner2&role=READER") ~>
      methodsService.namespaceAclsRoute ~>
      check {
        assert(status == OK)
        assert(!body.asString.contains(owner2))
      }

  }
  "Agora" should "not allow unauthorized users to delete an existing namespace ACL" in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + "acls" +
      s"?user=$owner2&role=OWNER") ~>
      methodsService.namespaceAclsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }


  "Agora" should "return entity ACL list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
        "/" + agoraEntity1.snapshotId.get + "/" + "acls") ~>
      methodsService.entityAclsRoute ~>
      check {
        assert(status == OK)
      }
  }

  "Agora" should "not return entity ACL list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "acls") ~>
      methodsService.entityAclsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

  "Agora" should "allow authorized users to insert a entity ACL" in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "acls" + s"?user=$owner2&role=OWNER") ~>
      methodsService.entityAclsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString.contains(owner2))
      }
  }

  "Agora" should "not allow unauthorized users to insert a entity ACL" in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "acls" + s"?user=$owner1&role=OWNER") ~>
      methodsService.entityAclsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

  "Agora" should "allow authorized users to edit an existing entity ACL" in {

    Post(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "acls" + s"?user=$owner2&role=READER") ~>
      methodsService.entityAclsRoute ~>
      check {
        assert(status == OK)
        assert(body.asString.contains("READER"))
      }
  }

  "Agora" should "allow authorized users to delete an existing entity ACL" in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "acls" + s"?user=$owner2&role=OWNER") ~>
      methodsService.entityAclsRoute ~>
      check {
        assert(status == OK)
        assert(!body.asString.contains(owner2))
      }
  }

  "Agora" should "not allow unauthorized users to delete an existing entity ACL" in {

    Delete(ApiUtil.Methods.withLeadingSlash + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "acls" + s"?user=$owner2&role=OWNER") ~>
      methodsService.entityAclsRoute ~>
      check {
        assert(status == Unauthorized)
      }
  }

}
