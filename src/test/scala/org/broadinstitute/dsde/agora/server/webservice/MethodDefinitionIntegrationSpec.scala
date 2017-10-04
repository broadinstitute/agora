package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions, EntityAccessControl}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, MethodDefinition}
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import spray.testkit.{RouteTest, ScalatestRouteTest}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.duration._

@DoNotDiscover
class MethodDefinitionIntegrationSpec extends FlatSpec with RouteTest with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory: ActorSystem = system
  }

  val methodsService = new MethodsService(permsDataSource) with ActorRefFactoryContext

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()

    // create a few unique methods, each with multiple snapshots
    patiently(agoraBusiness.insert(testMethod("one",1), mockAuthenticatedOwner.get))
    for (x <- 1 to 2)
      patiently(agoraBusiness.insert(testMethod("two",x), mockAuthenticatedOwner.get))
    for (x <- 1 to 3)
      patiently(agoraBusiness.insert(testMethod("three",x), mockAuthenticatedOwner.get))
    // this one will have a redacted snapshot
    for (x <- 1 to 4)
      patiently(agoraBusiness.insert(testMethod("redacts",x), mockAuthenticatedOwner.get))
    // this will have a snapshot owned by someone else
    for (x <- 1 to 5)
      patiently(agoraBusiness.insert(testMethod("otherowner",x), mockAuthenticatedOwner.get))

    // create some configs
    // method 1 has 1 config
    patiently(agoraBusiness.insert(testConfig("one",1), mockAuthenticatedOwner.get))
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

  behavior of "Agora method definitions listing"

  it should "return OK" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
    }
  }

  it should "count method snapshots" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
        val defs = responseAs[Seq[MethodDefinition]]
        val one = findDefinition("one", defs)
        assert(one.isDefined)
        assertResult(1) {one.get.numSnapshots}
        val two = findDefinition("two", defs)
        assert(two.isDefined)
        assertResult(2) {two.get.numSnapshots}
        val three = findDefinition("three", defs)
        assert(three.isDefined)
        assertResult(3) {three.get.numSnapshots}
      }
  }

  it should "respect permissions on methods" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
        val defs = responseAs[Seq[MethodDefinition]]
        val redacts = findDefinition("redacts", defs)
        assert(redacts.isDefined)
        assertResult(3) {redacts.get.numSnapshots}
        val otherowner = findDefinition("otherowner", defs)
        assert(otherowner.isDefined)
        assertResult(4) {otherowner.get.numSnapshots}
      }
  }

  it should "count associated configurations" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
        val defs = responseAs[Seq[MethodDefinition]]
        val one = findDefinition("one", defs)
        assert(one.isDefined)
        assertResult(1) {one.get.numConfigurations}
        val two = findDefinition("two", defs)
        assert(two.isDefined)
        assertResult(2) {two.get.numConfigurations}
        val three = findDefinition("three", defs)
        assert(three.isDefined)
        assertResult(2) {three.get.numConfigurations}
      }
  }

  it should "respect permissions on configurations" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
        val defs = responseAs[Seq[MethodDefinition]]
        val redacts = findDefinition("redacts", defs)
        assert(redacts.isDefined)
        // has 4 configs, one of which points at the redacted snapshot
        // and one of which is itself redacted
        assertResult(3) {redacts.get.numConfigurations}
        val otherowner = findDefinition("otherowner", defs)
        assert(otherowner.isDefined)
        // has 5 configs, one of which points at the snapshot owned
        // by somebody else, and one which is itself owned by somebody else
        assertResult(4) {otherowner.get.numConfigurations}
      }
  }

  it should "return the appropriate synopsis" in {
    // synopsis is pulled from most recent snapshot
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
        val defs = responseAs[Seq[MethodDefinition]]
        val one = findDefinition("one", defs)
        assert(one.isDefined)
        assert(one.get.synopsis.contains("synopsis-1"))
        val two = findDefinition("two", defs)
        assert(two.isDefined)
        assert(two.get.synopsis.contains("synopsis-2"))
        val three = findDefinition("three", defs)
        assert(three.isDefined)
        assert(three.get.synopsis.contains("synopsis-3"))
        val redacts = findDefinition("redacts", defs)
        assert(redacts.isDefined)
        assert(redacts.get.synopsis.contains("synopsis-4"))
        val otherowner = findDefinition("otherowner", defs)
        assert(otherowner.isDefined)
        assert(otherowner.get.synopsis.contains("synopsis-5"))
      }
  }

  it should "return the appropriate public status" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
        val defs = responseAs[Seq[MethodDefinition]]
        val one = findDefinition("one", defs)
        assert(one.isDefined)
        assert(one.get.public.contains(false))
        val two = findDefinition("two", defs)
        assert(two.isDefined)
        assert(two.get.public.contains(true))
        val three = findDefinition("three", defs)
        assert(three.isDefined)
        assert(three.get.public.contains(false))
        val redacts = findDefinition("redacts", defs)
        assert(redacts.isDefined)
        assert(redacts.get.public.contains(true))
        val otherowner = findDefinition("otherowner", defs)
        assert(otherowner.isDefined)
        assert(otherowner.get.public.contains(false))
      }
  }

  it should "return the appropriate managers" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/definitions") ~>
      methodsService.queryMethodDefinitionsRoute ~>
      check {
        assert(status == OK)
        val defs = responseAs[Seq[MethodDefinition]]
        val one = findDefinition("one", defs)
        assert(one.isDefined)
        assertResult(Set(mockAuthenticatedOwner.get)) {one.get.managers.toSet}
        val two = findDefinition("two", defs)
        assert(two.isDefined)
        assertResult(Set(mockAuthenticatedOwner.get)) {two.get.managers.toSet}
        val three = findDefinition("three", defs)
        assert(three.isDefined)
        assertResult(Set(mockAuthenticatedOwner.get)) {three.get.managers.toSet}
        val redacts = findDefinition("redacts", defs)
        assert(redacts.isDefined)
        assertResult(Set(mockAuthenticatedOwner.get)) {redacts.get.managers.toSet}
        val otherowner = findDefinition("otherowner", defs)
        assert(otherowner.isDefined)
        assertResult(Set(mockAuthenticatedOwner.get, owner1.get, owner3.get)) {otherowner.get.managers.toSet}
      }
  }

  // =========================================================
  // =================== helper methods
  // =========================================================
  private def testMethod(label:String): AgoraEntity=
    testMethod(label,0)

  private def testMethod(label:String,counter:Int): AgoraEntity =
    testMethod(s"MethodDefinitionIntegrationSpec-ns-$label",
      s"MethodDefinitionIntegrationSpec-name-$label",
      s"synopsis-$counter")

  private def testMethod(namespace:String, name:String, synopsis:String): AgoraEntity =
    testIntegrationEntity.copy(namespace=Some(namespace),
      name=Some(name),
      synopsis=Some(synopsis))

  private def findDefinition(label:String, defs:Seq[MethodDefinition]): Option[MethodDefinition] = {
    defs.find( d =>
      d.namespace.contains(s"MethodDefinitionIntegrationSpec-ns-$label") &&
        d.name.contains(s"MethodDefinitionIntegrationSpec-name-$label")
    )
  }

  private def testConfig(label:String, methodSnapshotId:Int): AgoraEntity = {
    testAgoraConfigurationEntity.copy(
      namespace=Some(s"MethodDefinitionIntegrationSpec-config-ns-$label"),
      name=Some(s"MethodDefinitionIntegrationSpec-config-name-$label"),
      payload=Some(testConfigPayload(label, methodSnapshotId))
    )
  }

  private def testConfigPayload(label:String, methodSnapshotId:Int): String =
     s"""{
     |  "methodRepoMethod": {
     |    "methodNamespace": "MethodDefinitionIntegrationSpec-ns-$label",
     |    "methodName": "MethodDefinitionIntegrationSpec-name-$label",
     |    "methodVersion": $methodSnapshotId
     |  },
     |  "prerequisites": {},
     |  "namespace": "confignamespace",
     |  "name": "configname",
     |  "outputs": {
     |
     |  },
     |  "inputs": {
     |    "p": "hi"
     |  },
     |  "rootEntityType": "sample"
     |}""".stripMargin



}


