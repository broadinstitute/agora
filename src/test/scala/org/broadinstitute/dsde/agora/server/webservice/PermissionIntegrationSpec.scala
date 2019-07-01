package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.directives.ExecutionDirectives
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions, EntityAccessControl}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}
import org.broadinstitute.dsde.agora.server.webservice.permissions.{EntityPermissionsService, MultiEntityPermissionsService, NamespacePermissionsService}
import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsValue, RootJsonFormat}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives

@DoNotDiscover
class PermissionIntegrationSpec extends FlatSpec with ScalatestRouteTest with BeforeAndAfterAll
  with AgoraTestFixture with ExecutionDirectives with SprayJsonSupport with DefaultJsonProtocol {

  val namespacePermissionsService = new NamespacePermissionsService(permsDataSource)
  val entityPermissionsService = new EntityPermissionsService(permsDataSource)
  val multiEntityPermissionsService = new MultiEntityPermissionsService(permsDataSource)
  val methodsService = new MethodsService(permsDataSource)

  val routes: Route = ApiService.handleExceptionsAndRejections {
    namespacePermissionsService.routes ~ entityPermissionsService.routes ~ multiEntityPermissionsService.routes ~
    methodsService.querySingleRoute
  }

  var agoraEntity1: AgoraEntity = _
  var agoraEntity2: AgoraEntity = _
  var agoraEntity3: AgoraEntity = _
  var redactedEntity: AgoraEntity = _

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    startMockWaas()

    agoraEntity1 = patiently(agoraBusiness.insert(testIntegrationEntity, mockAuthenticatedOwner.get, mockAccessToken))
    agoraEntity2 = patiently(agoraBusiness.insert(testIntegrationEntity2, owner2.get, mockAccessToken))
    agoraEntity3 = patiently(agoraBusiness.insert(testIntegrationEntity3, mockAuthenticatedOwner.get, mockAccessToken))

    redactedEntity = patiently(agoraBusiness.insert(testEntityToBeRedacted2, mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.delete(redactedEntity, Seq(redactedEntity.entityType.get), mockAuthenticatedOwner.get))
  }

  override def afterAll(): Unit = {
    clearDatabases()
    stopMockWaas()
  }

  "Agora" should "return namespace permissions. list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "Manage")
    }
  }

  "Agora" should "not return namespace permissions. list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + "permissions") ~>
      routes ~>
      check {
        assert(status == Forbidden)
      }
    }

  "Agora" should "allow authorized users to insert multiple roles in a single namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read,Create,Manage") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "Create")
      }
  }

  "Agora" should "not allow authorized users to POST over their own namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      routes ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "not allow unauthorized users to insert a namespace permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=All") ~>
      routes ~>
      check {
        assert(status == Forbidden)
      }
  }

  "Agora" should "only allow authorized users to overwrite existing permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "Read")
      }
  }

  "Agora" should "not allow authorized users to overwrite their own namespace permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      routes ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "allow authorized users to delete an existing namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=Read") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "[]")
      }
  }

  "Agora" should "not allow authorized users to delete their own namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + "permissions" +
      s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      routes ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "not allow unauthorized users to delete an existing namespace permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + "permissions" +
      s"?user=$owner2&roles=All") ~>
      routes ~>
      check {
        assert(status == Forbidden)
      }
  }


  "Agora" should "return entity permissions. list for authorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
        "/" + agoraEntity1.snapshotId.get + "/" + "permissions") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "Manage")
      }
  }

  "Agora" should "not return entity permissions. list for unauthorized users" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions") ~>
      routes ~>
      check {
        assert(status == Forbidden)
      }
  }

  "Agora" should "allow authorized users to insert a entity permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "Manage")
      }
  }

  "Agora" should "not allow authorized users to POST over their own entity permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      routes ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "not allow unauthorized users to insert a entity permissions." in {

    Post(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions" + s"?user=$agoraTestOwner&roles=All") ~>
      routes ~>
      check {
        assert(status == Forbidden)
      }
  }

  "Agora" should "allow authorized users to edit an existing entity permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=Read") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "Read")
      }
  }

  "Agora" should "not allow authorized users to edit their own entity permissions." in {

    Put(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=${mockAuthenticatedOwner.get}&roles=Read") ~>
      routes ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "allow authorized users to delete an existing entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      routes ~>
      check {
        assert(status == OK)
        assert(responseAs[String] contains "[]")
      }
  }

  "Agora" should "not allow authorized users to delete their own entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" + agoraEntity1.name.get +
      "/" + agoraEntity1.snapshotId.get + "/" + "permissions" + s"?user=${mockAuthenticatedOwner.get}&roles=All") ~>
      routes ~>
      check {
        assert(status == BadRequest)
      }
  }

  "Agora" should "not allow unauthorized users to delete an existing entity permissions." in {

    Delete(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" + agoraEntity2.name.get +
      "/" + agoraEntity2.snapshotId.get + "/" + "permissions" + s"?user=$owner2&roles=All") ~>
      routes ~>
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
      routes ~>
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

  "Agora" should "return owners when listing permissions for multiple methods simultaneously" in {
    val payload: Seq[AgoraEntity] = Seq(
      AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, agoraEntity1.snapshotId),
      AgoraEntity(agoraEntity2.namespace, agoraEntity2.name, agoraEntity2.snapshotId),
      AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, Some(12345)),
      AgoraEntity(redactedEntity.namespace, redactedEntity.name, redactedEntity.snapshotId)
    )

    // make sure mockAuthenticatedOwner has read permissions (not manage) on agoraEntity2
    patiently(permissionBusiness.insertEntityPermission(agoraEntity2, owner2.get,
      AccessControl(mockAuthenticatedOwner.get, AgoraPermissions(AgoraPermissions.Read))))


    Post(ApiUtil.Methods.withLeadingVersion + "/permissions", payload) ~>
      routes ~>
      check {
        assert(status == OK)

        // we hack the json deserialization here, to ensure we can read managers from the http response.
        // the standard AgoraEntityFormat does NOT read managers from json, and we explicitly do NOT want
        // to do this for the rest of the application, because owners should always be looked up from the db,
        // not read from json. However, we do it here in the unit test to validate correctness.
        // NB: I couldn't get this to work by importing AgoraEntityFormatWithManagerRead as an implicit.
        val rawJs = responseAs[JsArray]
        val entityAclList = rawJs.elements.map { jsv =>
          val js = jsv.asJsObject
          val entityJs = js.fields("entity").asJsObject
          val eac = js.convertTo[EntityAccessControl]
          eac.copy(entity = AgoraApiJsonSupportWithManagerAndPublicRead.AgoraEntityFormatWithManagerAndPublicRead.read(entityJs))
        }

        assertResult(4) {entityAclList.size}

        // check first - should get permissions
        {
          val stubEntity = AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, agoraEntity1.snapshotId)
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "first")
          assertResult(Set(mockAuthenticatedOwner.get), "first") {found.get.entity.managers.toSet}
        }
        // check second - it exists, but we only have read on it
        {
          val stubEntity = AgoraEntity(agoraEntity2.namespace, agoraEntity2.name, agoraEntity2.snapshotId)
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "second")
          assertResult(Set(owner2.get), "second") {found.get.entity.managers.toSet}
        }
        // check third - it doesn't exist in the db
        {
          val stubEntity = AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, Some(12345))
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "third")
          assert(found.get.entity.managers.isEmpty, "third") // entity doesn't exist, so it has no managers
        }
        // check fourth - it has been redacted, which resolves to us not having permissions to see it
        {
          val stubEntity = AgoraEntity(redactedEntity.namespace, redactedEntity.name, redactedEntity.snapshotId)
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "fourth")
          assert(found.get.entity.managers.isEmpty, "fourth") // when redacted, nobody owns it
        }
      }
  }

  "Agora" should "return public info when listing permissions for multiple methods simultaneously" in {
    val payload: Seq[AgoraEntity] = Seq(
      AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, agoraEntity1.snapshotId),
      AgoraEntity(agoraEntity2.namespace, agoraEntity2.name, agoraEntity2.snapshotId),
      AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, Some(12345)),
      AgoraEntity(redactedEntity.namespace, redactedEntity.name, redactedEntity.snapshotId)
    )

    // make sure public has read permissions (not manage) on agoraEntity2
    patiently(permissionBusiness.insertEntityPermission(agoraEntity2, owner2.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))


    Post(ApiUtil.Methods.withLeadingVersion + "/permissions", payload) ~>
      routes ~>
      check {
        assert(status == OK)

        // we hack the json deserialization here, to ensure we can read managers from the http response.
        // the standard AgoraEntityFormat does NOT read managers from json, and we explicitly do NOT want
        // to do this for the rest of the application, because owners should always be looked up from the db,
        // not read from json. However, we do it here in the unit test to validate correctness.
        // NB: I couldn't get this to work by importing AgoraEntityFormatWithManagerRead as an implicit.
        val rawJs = responseAs[JsArray]
        val entityAclList = rawJs.elements.map { jsv =>
          val js = jsv.asJsObject
          val entityJs = js.fields("entity").asJsObject
          val eac = js.convertTo[EntityAccessControl]
          eac.copy(entity = AgoraApiJsonSupportWithManagerAndPublicRead.AgoraEntityFormatWithManagerAndPublicRead.read(entityJs))
        }

        assertResult(4) {entityAclList.size}

        // check first - should get permissions
        {
          val stubEntity = AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, agoraEntity1.snapshotId)
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "first")
          assertResult(Some(false)) {found.get.entity.public}
        }
        // check second - it exists, but we only have read on it
        {
          val stubEntity = AgoraEntity(agoraEntity2.namespace, agoraEntity2.name, agoraEntity2.snapshotId)
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "second")
          assertResult(Some(true)) {found.get.entity.public}
        }
        // check third - it doesn't exist in the db
        {
          val stubEntity = AgoraEntity(agoraEntity1.namespace, agoraEntity1.name, Some(12345))
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "third")
          assertResult(Some(false)) {found.get.entity.public} // entity doesn't exist, so it's not public
        }
        // check fourth - it has been redacted, which resolves to us not having permissions to see it
        {
          val stubEntity = AgoraEntity(redactedEntity.namespace, redactedEntity.name, redactedEntity.snapshotId)
          val found = entityAclList.find(_.entity.toShortString == stubEntity.toShortString)
          assert(found.isDefined, "fourth")
          assertResult(Some(false)) {found.get.entity.public} // when redacted, so it's not public
        }
      }
  }

  "Agora" should "successfully upsert permissions for multiple methods and users simultaneously" in {

    // initial state should start with no permissions for our test cases
    assertResult(None) {getUserPermissions(agoraEntity1, owner2.get)(mockAuthenticatedOwner.get)}
    assertResult(None) {getUserPermissions(agoraEntity1, owner3.get)(mockAuthenticatedOwner.get)}
    assertResult(None) {getUserPermissions(agoraEntity3, owner3.get)(mockAuthenticatedOwner.get)}

    val payload:Seq[EntityAccessControl] = Seq(
      EntityAccessControl(agoraEntity1, Seq(AccessControl((owner2.get, AgoraPermissions.Read)))),
      EntityAccessControl(agoraEntity1, Seq(AccessControl((owner3.get, AgoraPermissions.Write)))),
      EntityAccessControl(agoraEntity3, Seq(AccessControl((owner3.get, AgoraPermissions.Manage))))
    )

    Put(ApiUtil.Methods.withLeadingVersion + "/permissions", payload) ~>
      routes ~>
      check {
        assert(status == OK)
        assertResult(Some(AgoraPermissions(AgoraPermissions.Read)), "owner 2 on entity 1") {getUserPermissions(agoraEntity1, owner2.get)(mockAuthenticatedOwner.get)}
        assertResult(Some(AgoraPermissions(AgoraPermissions.Write)), "owner 3 on entity 1") {getUserPermissions(agoraEntity1, owner3.get)(mockAuthenticatedOwner.get)}
        assertResult(Some(AgoraPermissions(AgoraPermissions.Manage)), "owner 3 on entity 3") {getUserPermissions(agoraEntity3, owner3.get)(mockAuthenticatedOwner.get)}
      }
  }

  "Agora" should "upsert some and return error messages for others on multiple-acl endpoint" in {
    // initial state should start with no permissions for our positive test case
    assertResult(None) {getUserPermissions(agoraEntity1, adminUser.get)(mockAuthenticatedOwner.get)}

    val theseShouldFail = Seq(
      // this should fail - can't change my own permissions
      EntityAccessControl(agoraEntity1, Seq(AccessControl((mockAuthenticatedOwner.get, AgoraPermissions.Read)))),
      // this should fail - I don't have manage on agoraEntity2
      EntityAccessControl(agoraEntity2, Seq(AccessControl((owner3.get, AgoraPermissions.Manage)))),
      // this should fail - it was redacted
      EntityAccessControl(redactedEntity, Seq(AccessControl((owner3.get, AgoraPermissions.Manage))))
    )

    // cache pre-existing acls for the negative test case
    val preExistingSelf = getUserPermissions(agoraEntity1, mockAuthenticatedOwner.get)(mockAuthenticatedOwner.get)

    val payload:Seq[EntityAccessControl] = theseShouldFail :+
      // this one should succeed
      EntityAccessControl(agoraEntity1, Seq(AccessControl((adminUser.get, AgoraPermissions.Write))))

    Put(ApiUtil.Methods.withLeadingVersion + "/permissions", payload) ~>
      routes ~>
      check {
        assert(status == OK)
        val resp = responseAs[Seq[EntityAccessControl]]
        theseShouldFail foreach { eac =>
          assert(getResponseMessage(resp, eac).isDefined)
          val expected = if (eac.acls.head.user == owner3.get)
            None
          else
            preExistingSelf
          assertResult(expected) {getUserPermissions(eac.entity, eac.acls.head.user)(mockAuthenticatedOwner.get)}
        }
      }
  }

  "Agora" should "set the `public` field to true for a public entity" in {

    patiently(permissionBusiness.insertEntityPermission(agoraEntity2, owner2.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity2.namespace.get + "/" +
      agoraEntity2.name.get + "/" + agoraEntity2.snapshotId.get) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
        val rawJs = responseAs[JsObject]
        val entity = AgoraApiJsonSupportWithManagerAndPublicRead.AgoraEntityFormatWithManagerAndPublicRead.read(rawJs)

        assert(status == OK)
        assert(entity.public.contains(true))
      }
  }

  "Agora" should "set the `public` field to false for a non-public entity" in {

    Get(ApiUtil.Methods.withLeadingVersion + "/" + agoraEntity1.namespace.get + "/" +
      agoraEntity1.name.get + "/" + agoraEntity1.snapshotId.get) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
        val rawJs = responseAs[JsObject]
        val entity = AgoraApiJsonSupportWithManagerAndPublicRead.AgoraEntityFormatWithManagerAndPublicRead.read(rawJs)

        assert(status == OK)
        assert(entity.public.contains(false))
      }
  }

  private def getUserPermissions(entity: AgoraEntity, userToCheck: String)(requester: String): Option[AgoraPermissions] = {
    val allAcls = patiently(permissionBusiness.listEntityPermissions(entity, requester) recover {
      case e:Exception => Seq.empty[AccessControl] })
    allAcls.find(_.user == userToCheck).map(_.roles)
  }

  private def getResponseMessage(resp: Seq[EntityAccessControl], criteria:EntityAccessControl): Option[String] = {
    // val foundEntity = resp.find(x => x.entity.toShortString == criteria.entity.toShortString && x.acls == criteria.acls)
    val foundEntity = resp.find(x =>
      x.entity.toShortString == criteria.entity.toShortString &&
      x.acls.head.user == criteria.acls.head.user)
    val returnMessage = foundEntity.flatMap(_.message)
    returnMessage
  }

}

object AgoraApiJsonSupportWithManagerAndPublicRead extends DefaultJsonProtocol {

  implicit object AgoraEntityFormatWithManagerAndPublicRead extends RootJsonFormat[AgoraEntity] {
    override def write(entity: AgoraEntity) = AgoraEntityFormat.write(entity)

    override def read(json: JsValue): AgoraEntity = {
      val entityWithoutManagers = AgoraEntityFormat.read(json)
      val jsObject = json.asJsObject
      val managers = if (jsObject.getFields("managers").nonEmpty) jsObject.fields("managers").convertTo[Seq[String]] else Seq.empty[String]
      val isPublic = if (jsObject.getFields("public").nonEmpty) jsObject.fields("public").convertTo[Option[Boolean]] else None
      entityWithoutManagers.addManagers(managers).copy(public=isPublic)
    }
  }

}
