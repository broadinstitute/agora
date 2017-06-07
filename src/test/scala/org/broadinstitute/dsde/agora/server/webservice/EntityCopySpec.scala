
package org.broadinstitute.dsde.agora.server.webservice

import java.util.UUID

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import spray.http.{HttpResponse, StatusCode, Uri}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

import scala.concurrent.Future

@DoNotDiscover
class EntityCopySpec extends ApiServiceSpec {

  var testEntity1WithId: AgoraEntity = _

  val testRoute = ApiUtil.Methods.withLeadingVersion + "/%s/%s/%s"

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    // create entity1, owned by owner1
    testEntity1WithId = patiently(agoraBusiness.insert(testEntity1, owner1.get))
    // add owner2 as reader on entity1
    patiently(permissionBusiness.insertEntityPermission(testEntity1WithId, owner1.get, AccessControl(owner2.get, AgoraPermissions(Read))))
  }

  override def afterAll() = {
    clearDatabases()
  }

  behavior of "Agora, when creating/editing entities (methods and configs)"

  it should "set up test fixtures correctly" in {
    val actualPerms = patiently(permissionBusiness.listEntityPermissions(testEntity1WithId, owner1.get))

    val expectedPerms = List(
      AccessControl(owner1.get, AgoraPermissions(All)),
      AccessControl(owner2.get, AgoraPermissions(Read))
    )

    assertResult(expectedPerms) {actualPerms}

  }

  it should "reject copying a configuration" in {
    val targetUri = ApiUtil.Configurations.withLeadingVersion + "/nosupportfor/configs/1"
    Post(targetUri, AgoraEntity()) ~>
      addHeader(MockAgoraDirectives.mockAuthenticatedUserEmailHeader, owner1.get) ~>
      configurationsService.querySingleRoute ~> check {
        assert(!handled)
    }
  }

  it should "fail when source does not exist" in {
    testEntityCopy(NotFound, owner1.get, "doesnt", "exist", 1)
  }

  it should "fail when source exists but I don't have Create on it" in {
    // owner 2 only has read permissions on the entity
    testEntityCopy(Forbidden, owner2.get, namespace1.get, name1.get, 1)
  }

  it should "create an identical copy" in {
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1)
  }

  it should "create a copy with my overrides" in {
    val arg1 = AgoraEntity(synopsis=Some(randUUID))
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1, arg1)
    val arg2 = AgoraEntity(documentation=Some(randUUID))
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1, arg2)
    val arg3 = AgoraEntity(payload=payload2)
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1, arg3)
    val arg4 = AgoraEntity(synopsis=Some(randUUID), documentation=Some(randUUID), payload=payloadWcTask)
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1, arg4)
  }

  it should "not redact the source entity by default" in {
    patiently(testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1))
    // following will throw an exception if the entity could not be found, i.e. was redacted
    patiently(agoraBusiness.findSingle(namespace1.get, name1.get, 1, Seq(AgoraEntityType.Workflow), owner1.get))
  }

  it should "redact the source entity when requested" in {
    // create a copy; we'll use this copy to redact so we don't interrupt other tests in this class
    val latestSnap = patiently(testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1, redact=false))

    latestSnap.entity.as[AgoraEntity] match {
      case Left(_) => fail(latestSnap.message.toString)
      case Right(ae:AgoraEntity) => {
        // testEntityCopy(OK, owner1.get, namespace1.get, name1.get, ae.snapshotId.get, redact=true)
        /*
        intercept[AgoraEntityNotFoundException] {
          agoraBusiness.findSingle(namespace1.get, name1.get, ae.snapshotId.get, Seq(AgoraEntityType.Workflow), owner1.get)
        }
        */
      }
    }

    fail("not implemented")
  }

  it should "throw error when failing to create target" in {
    fail("not implemented")
  }

  it should "throw error when failing to copy permissions" in {
    fail("not implemented")
  }

  it should "redact the target when failing to copy permissions" in {
    fail("not implemented")
  }
  // =============================================================================================
  // support methods
  // =============================================================================================

  private def testEntityCopy(expectedStatus: StatusCode, asUser: String,
                             namespace: String, name: String, snapshotId: Int,
                             argument: AgoraEntity = AgoraEntity(), redact: Boolean = false): Future[HttpResponse] = {

    val targetUri =
      Uri(testRoute.format(namespace,name,snapshotId)).withQuery(Map("redact"->redact.toString))

    Post(targetUri, AgoraEntity()) ~>
      addHeader(MockAgoraDirectives.mockAuthenticatedUserEmailHeader, asUser) ~>
      methodsService.querySingleRoute ~> check {
        assert(status == expectedStatus, response.message)
        if (expectedStatus == Created) {
          handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
            assert(entity.namespace == testEntity1WithId.namespace)
            assert(entity.name == testEntity1WithId.name)
            assert(entity.owner == testEntity1WithId.owner)
            assert(entity.entityType == testEntity1WithId.entityType)
            assert(entity.snapshotId.isDefined)
            assert(entity.createDate.isDefined)
            if (argument.synopsis.isDefined)
              assert(entity.synopsis == argument.synopsis)
            else
              assert(entity.synopsis == testEntity1WithId.synopsis)
            if (argument.documentation.isDefined)
              assert(entity.documentation == argument.documentation)
            else
              assert(entity.documentation == testEntity1WithId.documentation)
            if (argument.payload.isDefined)
              assert(entity.payload == argument.payload)
            else
              assert(entity.payload == testEntity1WithId.payload)
          })
        }
        Future.successful(response)
    }
  }

  private def randUUID: String = UUID.randomUUID.toString

}
