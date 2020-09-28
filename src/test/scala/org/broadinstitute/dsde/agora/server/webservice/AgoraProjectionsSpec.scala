
package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import org.scalatest.flatspec.AnyFlatSpecLike

@DoNotDiscover
class AgoraProjectionsSpec extends ApiServiceSpec with AnyFlatSpecLike {

  var testEntity1WithId: AgoraEntity = _
  var testEntity2WithId: AgoraEntity = _
  var testEntity3WithId: AgoraEntity = _
  var testEntity4WithId: AgoraEntity = _
  var testEntity5WithId: AgoraEntity = _
  var testEntity6WithId: AgoraEntity = _
  var testEntity7WithId: AgoraEntity = _
  var testEntityToBeRedactedWithId: AgoraEntity = _

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    startMockWaas()

    testEntity1WithId = patiently(agoraBusiness.insert(testEntity1, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity2WithId = patiently(agoraBusiness.insert(testEntity2, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity3WithId = patiently(agoraBusiness.insert(testEntity3, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity4WithId = patiently(agoraBusiness.insert(testEntity4, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity5WithId = patiently(agoraBusiness.insert(testEntity5, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity6WithId = patiently(agoraBusiness.insert(testEntity6, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity7WithId = patiently(agoraBusiness.insert(testEntity7, mockAuthenticatedOwner.get, mockAccessToken))
    testEntityToBeRedactedWithId = patiently(agoraBusiness.insert(testEntityToBeRedacted, mockAuthenticatedOwner.get, mockAccessToken))
  }

  override def afterAll(): Unit = {
    clearDatabases()
    stopMockWaas()
  }

  "Agora" should "return only included fields in the entity" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?namespace=" + namespace1.get + "&name=" + name2.get + "&includedField=name&includedField=snapshotId&includedField=snapshotComment") ~>
      methodsService.queryRoute ~> check {
        val entities = responseAs[Seq[AgoraEntity]]
        assert(entities.toSet === includeProjection(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)).toSet)
        assert(status === OK)
      }
  }

  "Agora" should "not return excluded fields in the entity" in {
    Get(ApiUtil.Methods.withLeadingVersion
      + "?namespace=" + namespace1.get
      + "&name=" + name2.get
      + "&excludedField=synopsis&excludedField=documentation&excludedField=createDate&excludedField=payload&excludedField=snapshotComment") ~>
      methodsService.queryRoute ~> check {
        val entities = responseAs[Seq[AgoraEntity]]
        assert(entities.toSet === excludeProjection(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)).toSet)
        assert(status === OK)
      }
  }

  "Agora" should "reject the request if you specify required field as an excluded field" in {
    Get(ApiUtil.Methods.withLeadingVersion
      + "?namespace=" + namespace1.get
      + "&name=" + name2.get
      + "&excludedField=namespace&excludedField=snapshotId") ~>
      wrapWithExceptionHandler {
        methodsService.queryRoute
      } ~> check {
      assert(status === BadRequest)
    }
  }

  "Agora" should "reject the request if you specify both excludedField and includedField" in {
    Get(ApiUtil.Methods.withLeadingVersion
      + "?namespace=" + namespace1.get
      + "&name=" + name2.get
      + "&excludedField=synopsis&includedField=documentation") ~>
      wrapWithExceptionHandler {
        methodsService.queryRoute
      } ~> check {
      assert(status === BadRequest)
    }
  }

}
