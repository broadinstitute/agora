
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

@DoNotDiscover
class AgoraProjectionsSpec extends ApiServiceSpec {

  var testEntity1WithId: AgoraEntity = _
  var testEntity2WithId: AgoraEntity = _
  var testEntity3WithId: AgoraEntity = _
  var testEntity4WithId: AgoraEntity = _
  var testEntity5WithId: AgoraEntity = _
  var testEntity6WithId: AgoraEntity = _
  var testEntity7WithId: AgoraEntity = _
  var testEntityToBeRedactedWithId: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    testEntity1WithId = agoraBusiness.insert(testEntity1, mockAutheticatedOwner.get)
    testEntity2WithId = agoraBusiness.insert(testEntity2, mockAutheticatedOwner.get)
    testEntity3WithId = agoraBusiness.insert(testEntity3, mockAutheticatedOwner.get)
    testEntity4WithId = agoraBusiness.insert(testEntity4, mockAutheticatedOwner.get)
    testEntity5WithId = agoraBusiness.insert(testEntity5, mockAutheticatedOwner.get)
    testEntity6WithId = agoraBusiness.insert(testEntity6, mockAutheticatedOwner.get)
    testEntity7WithId = agoraBusiness.insert(testEntity7, mockAutheticatedOwner.get)
    testEntityToBeRedactedWithId = agoraBusiness.insert(testEntityToBeRedacted, mockAutheticatedOwner.get)
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "return only included fields in the entity" in {
    Get(ApiUtil.Methods.withLeadingSlash + "?namespace=" + namespace1.get + "&name=" + name2.get + "&includedField=name&includedField=snapshotId") ~>
      methodsService.queryRoute ~> check {
      handleError(
        entity.as[Seq[AgoraEntity]],
        (entities: Seq[AgoraEntity]) =>
          assert(entities.toSet === includeProjection(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)).toSet)
      )
      assert(status === OK)
    }
  }

  "Agora" should "not return excluded fields in the entity" in {
    Get(ApiUtil.Methods.withLeadingSlash
      + "?namespace=" + namespace1.get
      + "&name=" + name2.get
      + "&excludedField=synopsis&excludedField=documentation&excludedField=createDate&excludedField=payload") ~>
      methodsService.queryRoute ~> check {
      handleError(
        entity.as[Seq[AgoraEntity]],
        (entities: Seq[AgoraEntity]) =>
          assert(entities.toSet === excludeProjection(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)).toSet)
      )
      assert(status === OK)
    }
  }

  "Agora" should "reject the request if you specify required field as an excluded field" in {
    Get(ApiUtil.Methods.withLeadingSlash
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
    Get(ApiUtil.Methods.withLeadingSlash
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
