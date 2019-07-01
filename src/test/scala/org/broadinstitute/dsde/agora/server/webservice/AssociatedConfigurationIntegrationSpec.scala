package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.directives.ExecutionDirectives
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}

import scala.concurrent.duration._

@DoNotDiscover
class AssociatedConfigurationIntegrationSpec extends FlatSpec with ExecutionDirectives
  with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory: ActorSystem = system
  }

  val methodsService = new MethodsService(permsDataSource) with ActorRefFactoryContext
  val testRoutes = ApiService.handleExceptionsAndRejections (methodsService.queryAssociatedConfigurationsRoute)

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    startMockWaas()

    // create a few unique methods, each with multiple snapshots
    patiently(agoraBusiness.insert(testMethod("one"), mockAuthenticatedOwner.get, mockAccessToken))
    for (x <- 1 to 2)
      patiently(agoraBusiness.insert(testMethod("two"), mockAuthenticatedOwner.get, mockAccessToken))
    for (x <- 1 to 3)
      patiently(agoraBusiness.insert(testMethod("three"), mockAuthenticatedOwner.get, mockAccessToken))
    // this one will have a redacted snapshot
    for (x <- 1 to 4)
      patiently(agoraBusiness.insert(testMethod("redacts"), mockAuthenticatedOwner.get, mockAccessToken))
    // this will have a snapshot owned by someone else
    for (x <- 1 to 5)
      patiently(agoraBusiness.insert(testMethod("otherowner"), mockAuthenticatedOwner.get, mockAccessToken))

    // create some configs
    // method 2 has 2 configs, both pointing at snapshot 1
    for (x <- 1 to 2)
      patiently(agoraBusiness.insert(testConfig("two",1), mockAuthenticatedOwner.get, mockAccessToken))
    // method 3 has 2 configs, pointing at snapshots 2 and 3
    patiently(agoraBusiness.insert(testConfig("three",2), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("three",3), mockAuthenticatedOwner.get, mockAccessToken))

    // method redacts has 4 configs, one of which points at the to-be-redacted snapshot
    // and one of which is itself redacted
    patiently(agoraBusiness.insert(testConfig("redacts",1), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("redacts",1), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("redacts",2), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("redacts",3), mockAuthenticatedOwner.get, mockAccessToken))
    // method otherowner has 5 configs, one of which points at the snapshot to-be-owned
    // by somebody else, and one which is itself owned by somebody else
    patiently(agoraBusiness.insert(testConfig("otherowner",1), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("otherowner",1), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("otherowner",2), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("otherowner",3), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("otherowner",4), mockAuthenticatedOwner.get, mockAccessToken))

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
    stopMockWaas()
  }

  behavior of "Agora associated configurations dlisting"

  it should "return OK for an existing method" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("one")) ~>
      testRoutes ~>
      check {
        assert(status == OK)
    }
  }

  it should "return 404 if no method found for the ns/n" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("nothinghere")) ~>
      testRoutes ~>
      check {
        assert(status == NotFound)
      }
  }

  it should "return the empty list when no configs are found for an existing method" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("one")) ~>
      testRoutes ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.isEmpty)
      }
  }

  it should "return multiple configs for the same method snapshot" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("two")) ~>
      testRoutes ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assertResult(2) {configs.size}

        assert(configs(0).payloadObject.get.name == payload_name)
        assert(configs(0).payloadObject.get.namespace == payload_namespace)
        assert(configs(1).payloadObject.get.name == payload_name)
        assert(configs(1).payloadObject.get.namespace == payload_namespace)

        assert(configs.forall(_.entityType.contains(AgoraEntityType.Configuration)))
        assert(configs.forall(_.name.get.contains("two")))
      }
  }

  it should "return multiple configs for different method snapshots with the same ns/n" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("three")) ~>
      testRoutes ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]

        assert(configs(0).payloadObject.get.name == payload_name)
        assert(configs(0).payloadObject.get.namespace == payload_namespace)
        assert(configs(1).payloadObject.get.name == payload_name)
        assert(configs(1).payloadObject.get.namespace == payload_namespace)

        assertResult(2) {configs.size}
        assert(configs.forall(_.entityType.contains(AgoraEntityType.Configuration)))
        assert(configs.forall(_.name.get.contains("three")))
      }
  }

  it should "honor permissions on method snapshots and configurations" in {
    Get(ApiUtil.Methods.withLeadingVersion + urlString("redacts")) ~>
      testRoutes ~>
      check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assertResult(2) {configs.size}
        assert(configs.forall(_.entityType.contains(AgoraEntityType.Configuration)))
        assert(configs.forall(_.name.get.contains("redacts")))
      }

    Get(ApiUtil.Methods.withLeadingVersion + urlString("otherowner")) ~>
      testRoutes ~>
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

  private val payload_name = "AssociatedConfigurationIntegrationSpec-config-payload-name"
  private val payload_namespace = "AssociatedConfigurationIntegrationSpec-config-payload-namespace"

  private def testConfigPayload(label:String, methodSnapshotId:Int): String =
     s"""{
     |  "name": "$payload_name",
     |  "namespace": "$payload_namespace",
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
     |  "rootEntityType": "sample",
     |  "prerequisites": {}
     |}""".stripMargin
}
