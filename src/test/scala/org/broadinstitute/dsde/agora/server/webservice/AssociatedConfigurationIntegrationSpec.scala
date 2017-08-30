package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.testkit.{RouteTest, ScalatestRouteTest}

import scala.concurrent.duration._

@DoNotDiscover
class AssociatedConfigurationIntegrationSpec extends FlatSpec with RouteTest with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory: ActorSystem = system
  }

  val methodsService = new MethodsService(permsDataSource) with ActorRefFactoryContext

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()

    // create a few unique methods, each with multiple snapshots
    patiently(agoraBusiness.insert(testMethod("one"), mockAuthenticatedOwner.get))
    for (x <- 1 to 2)
      patiently(agoraBusiness.insert(testMethod("two"), mockAuthenticatedOwner.get))
    for (x <- 1 to 3)
      patiently(agoraBusiness.insert(testMethod("three"), mockAuthenticatedOwner.get))
    // this one will have a redacted snapshot
    for (x <- 1 to 4)
      patiently(agoraBusiness.insert(testMethod("redacts"), mockAuthenticatedOwner.get))
    // this will have a snapshot owned by someone else
    for (x <- 1 to 5)
      patiently(agoraBusiness.insert(testMethod("otherowner"), mockAuthenticatedOwner.get))

    // create some configs
    // method 2 has 2 configs, both pointing at snapshot 1
    for (x <- 1 to 2)
      patiently(agoraBusiness.insert(testConfig("two",1), mockAuthenticatedOwner.get))
    // method 3 has 2 configs, pointing at snapshots 2 and 3
    patiently(agoraBusiness.insert(testConfig("three",2), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("three",3), mockAuthenticatedOwner.get))

    // method redacts has 4 configs, one of which points at the to-be-redacted snapshot
    // and one of which is itself redacted
    patiently(agoraBusiness.insert(testConfig("redacts",1), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("redacts",1), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("redacts",2), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("redacts",3), mockAuthenticatedOwner.get))
    // method otherowner has 5 configs, one of which points at the snapshot to-be-owned
    // by somebody else, and one which is itself owned by somebody else
    patiently(agoraBusiness.insert(testConfig("otherowner",1), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("otherowner",1), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("otherowner",2), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("otherowner",3), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("otherowner",4), mockAuthenticatedOwner.get))

    // redact a method snapshot
    patiently(agoraBusiness.delete(testMethod("redacts").copy(snapshotId = Some(2)), Seq(AgoraEntityType.Workflow), mockAuthenticatedOwner.get))

    // change perms on a method
    patiently(permissionBusiness.insertEntityPermission(testMethod("otherowner").copy(snapshotId = Some(2)), mockAuthenticatedOwner.get,
      AccessControl(owner2.get, AgoraPermissions(AgoraPermissions.All))))
    patiently(permissionBusiness.insertEntityPermission(testMethod("otherowner").copy(snapshotId = Some(2)), owner2.get,
      AccessControl(mockAuthenticatedOwner.get, AgoraPermissions(AgoraPermissions.Nothing))))

    // grant additional owners to a method
    patiently(permissionBusiness.insertEntityPermission(testMethod("otherowner").copy(snapshotId = Some(3)), mockAuthenticatedOwner.get,
      AccessControl(owner1.get, AgoraPermissions(AgoraPermissions.All))))
    patiently(permissionBusiness.insertEntityPermission(testMethod("otherowner").copy(snapshotId = Some(4)), mockAuthenticatedOwner.get,
      AccessControl(owner3.get, AgoraPermissions(AgoraPermissions.All))))

    // and add another reader to make sure readers don't confuse the system
    patiently(permissionBusiness.insertEntityPermission(testMethod("otherowner").copy(snapshotId = Some(4)), mockAuthenticatedOwner.get,
      AccessControl(owner2.get, AgoraPermissions(AgoraPermissions.Read))))

    // redact a config
    patiently(agoraBusiness.delete(testConfig("redacts",1).copy(snapshotId = Some(2)), Seq(AgoraEntityType.Configuration), mockAuthenticatedOwner.get))

    // change perms on a config
    patiently(permissionBusiness.insertEntityPermission(testConfig("otherowner",3).copy(snapshotId = Some(4)), mockAuthenticatedOwner.get,
      AccessControl(owner2.get, AgoraPermissions(AgoraPermissions.All))))
    patiently(permissionBusiness.insertEntityPermission(testConfig("otherowner",3).copy(snapshotId = Some(4)), owner2.get,
      AccessControl(mockAuthenticatedOwner.get, AgoraPermissions(AgoraPermissions.Nothing))))

    // grant public-read to a couple snapshots
    patiently(permissionBusiness.insertEntityPermission(testMethod("two").copy(snapshotId = Some(2)), mockAuthenticatedOwner.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))
    patiently(permissionBusiness.insertEntityPermission(testMethod("redacts").copy(snapshotId = Some(4)), mockAuthenticatedOwner.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))
  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  behavior of "Agora associated configurations dlisting"

  it should "return OK for an existing method" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("one")) ~>
      methodsService.queryAssociatedConfigurationsRoute ~>
      check {
        assert(status == OK)
    }
  }

  it should "return 404 if no method found for the ns/n" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("nothinghere")) ~>
      methodsService.queryAssociatedConfigurationsRoute ~>
      check {
        assert(status == NotFound)
      }
  }

  it should "return the empty list when no configs are found for an existing method" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("one")) ~>
      methodsService.queryAssociatedConfigurationsRoute ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.isEmpty)
      }
  }

  it should "return multiple configs for the same method snapshot" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("two")) ~>
      methodsService.queryAssociatedConfigurationsRoute ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assertResult(2) {configs.size}
        assert(configs.forall(_.entityType.contains(AgoraEntityType.Configuration)))
        assert(configs.forall(_.name.get.contains("two")))
      }
  }

  it should "return multiple configs for different method snapshots with the same ns/n" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("three")) ~>
      methodsService.queryAssociatedConfigurationsRoute ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assertResult(2) {configs.size}
        assert(configs.forall(_.entityType.contains(AgoraEntityType.Configuration)))
        assert(configs.forall(_.name.get.contains("three")))
      }
  }

  it should "honor permissions on method snapshots and configurations" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("redacts")) ~>
      methodsService.queryAssociatedConfigurationsRoute ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assertResult(2) {configs.size}
        assert(configs.forall(_.entityType.contains(AgoraEntityType.Configuration)))
        assert(configs.forall(_.name.get.contains("redacts")))
      }

    Get(ApiUtil.Methods.withLeadingVersion + urlString("otherowner")) ~>
      methodsService.queryAssociatedConfigurationsRoute ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assertResult(3) {configs.size}
        assert(configs.forall(_.entityType.contains(AgoraEntityType.Configuration)))
        assert(configs.forall(_.name.get.contains("otherowner")))
      }
  }


  // =========================================================
  // =================== helper methods
  // =========================================================
  private def urlString(label:String): String =
    s"/AssociatedConfigurationIntegrationSpec-ns-$label/AssociatedConfigurationIntegrationSpec-name-$label/configurations"

  private def testMethod(label:String): AgoraEntity =
    testMethod(s"AssociatedConfigurationIntegrationSpec-ns-$label",
      s"AssociatedConfigurationIntegrationSpec-name-$label")

  private def testMethod(namespace:String, name:String): AgoraEntity =
    testIntegrationEntity.copy(namespace=Some(namespace),
      name=Some(name))

  private def testConfig(label:String, methodSnapshotId:Int): AgoraEntity = {
    testAgoraConfigurationEntity.copy(
      namespace=Some(s"AssociatedConfigurationIntegrationSpec-config-ns-$label"),
      name=Some(s"AssociatedConfigurationIntegrationSpec-config-name-$label"),
      payload=Some(testConfigPayload(label, methodSnapshotId))
    )
  }

  private def testConfigPayload(label:String, methodSnapshotId:Int): String =
     s"""{
     |  "methodRepoMethod": {
     |    "methodNamespace": "AssociatedConfigurationIntegrationSpec-ns-$label",
     |    "methodName": "AssociatedConfigurationIntegrationSpec-name-$label",
     |    "methodVersion": $methodSnapshotId
     |  },
     |  "outputs": {
     |
     |  },
     |  "inputs": {
     |    "p": "hi"
     |  },
     |  "rootEntityType": "sample"
     |}""".stripMargin



}


