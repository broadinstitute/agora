
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.exceptions.AgoraEntityNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{DoNotDiscover, FlatSpecLike}
import org.scalatest.Matchers._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

@DoNotDiscover
class AgoraImportSpec extends ApiServiceSpec with FlatSpecLike{
  var testEntityTaskWcWithId: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    testEntityTaskWcWithId = patiently(agoraBusiness.insert(testEntityTaskWc, mockAuthenticatedOwner.get))
  }

  override def afterAll() = {
    clearDatabases()
  }

  "MethodsService" should "return a 400 when posting a WDL with an invalid import statement" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityInvalidWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
    }
  }

  "MethodsService" should "return a 400 when posting a WDL with an import statement that references a non-existent method" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityNonExistentWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
    }
  }

  "MethodsService" should "return 400 when the WDL contains an import, because imports are no longer allowed" in {
    Post(ApiUtil.Methods.withLeadingVersion, testEntityWorkflowWithExistentWdlImport) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
    }
  }
}
