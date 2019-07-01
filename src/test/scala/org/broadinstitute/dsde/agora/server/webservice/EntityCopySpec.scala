
package org.broadinstitute.dsde.agora.server.webservice

import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{DoNotDiscover, FlatSpecLike}

import scala.concurrent.Future

@DoNotDiscover
class EntityCopySpec extends ApiServiceSpec with FlatSpecLike {

  var testEntity1WithId: AgoraEntity = _
  var testEntityWithSnapshotComment: AgoraEntity = _

  val testRoute = ApiUtil.Methods.withLeadingVersion + "/%s/%s/%s"

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    startMockWaas()

    // create entity1, owned by owner1
    testEntity1WithId = patiently(agoraBusiness.insert(testEntity1, owner1.get, mockAccessToken))

    // add owner2 as reader on entity1
    patiently(permissionBusiness.insertEntityPermission(testEntity1WithId, owner1.get, AccessControl(owner2.get, AgoraPermissions(Read))))
    // add owner3 with Create (but not Redact) on entity1
    patiently(permissionBusiness.insertEntityPermission(testEntity1WithId, owner1.get, AccessControl(owner3.get, AgoraPermissions(Create))))

    testEntityWithSnapshotComment = patiently(agoraBusiness.insert(testMethodWithSnapshotComment1, owner1.get, mockAccessToken))
  }

  override def afterAll() = {
    clearDatabases()
    stopMockWaas()
  }

  behavior of "Agora, when creating/editing entities (methods and configs)"

  it should "set up test fixtures correctly" in {
    val actualPerms = patiently(permissionBusiness.listEntityPermissions(testEntity1WithId, owner1.get))

    val expectedPerms = List(
      AccessControl(owner1.get, AgoraPermissions(All)),
      AccessControl(owner2.get, AgoraPermissions(Read)),
      AccessControl(owner3.get, AgoraPermissions(Create))
    )

    assertResult(expectedPerms) {actualPerms}

  }

  it should "reject copying a configuration" in {
    val targetUri = ApiUtil.Configurations.withLeadingVersion + "/nosupportfor/configs/1"
    Post(targetUri, AgoraEntity()) ~>
      addHeader(MockAgoraDirectives.mockAuthenticatedUserEmailHeader, owner1.get) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~>
    ApiService.handleExceptionsAndRejections { configurationsService.querySingleRoute } ~> check {
        assert(!handled)
    }
  }

  it should "fail when source does not exist" in {
    testEntityCopy(NotFound, owner1.get, "doesnt", "exist", 1, assertRedact=false)
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
    val arg4 = AgoraEntity(snapshotComment = Some(randUUID))
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1, arg4)
    val arg5 = AgoraEntity(synopsis=Some(randUUID), documentation=Some(randUUID), payload=payloadWcTask, snapshotComment = Some(randUUID))
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1, arg5)
  }

  it should "not redact the source entity by default" in {
    testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 1)
  }

  it should "redact the source entity when requested" in {
    patiently(Future(testEntityCopy(OK, owner1.get, namespace1.get, name1.get, 2, redact=true)))
  }

  it should "return PartialContent if I can create but not redact" in {
    patiently(Future(testEntityCopy(PartialContent, owner3.get, namespace1.get, name1.get, 3, redact=true)))
  }

  it should "not copy snapshot comments" in {
    patiently(Future(testEntityCopy(OK, owner1.get, namespace3.get, name4.get, 1, testEntityWithSnapshotComment)))
  }


  // =============================================================================================
  // support methods
  // =============================================================================================

  private def testEntityCopy(expectedStatus: StatusCode, asUser: String,
                             namespace: String, name: String, snapshotId: Int,
                             argument: AgoraEntity = AgoraEntity(),
                             redact: Boolean = false, assertRedact: Boolean = true) = {

    val getUri: Uri = Uri(testRoute.format(namespace,name,snapshotId))
    val postUri = getUri.withQuery(Query(Map("redact" -> redact.toString)))

    Post(postUri, AgoraEntity()) ~>
      addHeader(MockAgoraDirectives.mockAuthenticatedUserEmailHeader, asUser) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~>
      ApiService.handleExceptionsAndRejections { methodsService.querySingleRoute } ~> check {
        assert(status == expectedStatus, response.toString)
        if (expectedStatus == Created) {
          val entity = responseAs[AgoraEntity]

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
          if (argument.snapshotComment.isDefined)
            assert(entity.snapshotComment == argument.snapshotComment)
          else
            assert(entity.snapshotComment.isEmpty)
        }
      }

    if (assertRedact) {
      Get(getUri) ~>
        addHeader(MockAgoraDirectives.mockAuthenticatedUserEmailHeader, asUser) ~>
        addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~>
        ApiService.handleExceptionsAndRejections { methodsService.querySingleRoute } ~> check {
          if (redact) {
            assert(status == NotFound, response.toString)
          } else {
            assert(status == OK, response.toString)
          }
        }
    }
  }

  private def randUUID: String = UUID.randomUUID.toString

}
