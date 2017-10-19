
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
  var testAgoraEntityWithId: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    testAgoraEntityWithId = patiently(agoraBusiness.insert(testAgoraEntity, mockAuthenticatedOwner.get))
  }

  override def afterAll() = {
    clearDatabases()
  }

  // tests for creating new methods
  "MethodsService" should "return 400 when posting a gibberish WDL" in {
    Post(ApiUtil.Methods.withLeadingVersion, copyPayload(Some("this isn't valid WDL!"))) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
    }
  }

  "MethodsService" should "return a 400 when posting a WDL with an invalid import statement" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityInvalidWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status == InternalServerError)
      assert(body.asString contains "Failed to import workflow invalid_syntax_for_tool.:\\ninvalid_syntax_for_tool:")
    }
  }

  "MethodsService" should "return a 400 when posting a WDL with an import statement that references a non-existent method" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityNonExistentWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status == InternalServerError)
      assert(body.asString contains "Failed to import workflow http://broad.non_existent_grep.1.:\\nbroad.non_existent_grep.1:")
    }
  }

  // tests for copying a method and specifying new WDL for the copy

  private val copyUrl = ApiUtil.Methods.withLeadingVersion +
    s"/${testAgoraEntity.namespace.get}/${testAgoraEntity.name.get}/1"


  "MethodsService" should "return 400 when copying a method and specifying a gibberish WDL" in {
    Post(copyUrl, copyPayload(Some("this isn't valid WDL!"))) ~>
      methodsService.querySingleRoute ~> check {
      assert(status == BadRequest)
    }
  }

  "MethodsService" should "return a 500 when copying a method and specifying a WDL with an invalid import statement" in {
    Post(copyUrl, copyPayload(testBadAgoraEntityInvalidWdlImportFormat.payload)) ~>
      methodsService.querySingleRoute ~> check {
      assert(status == InternalServerError)
      assert(body.asString contains "Failed to import workflow invalid_syntax_for_tool.:\\ninvalid_syntax_for_tool")
    }
  }

  "MethodsService" should "return a 500 when copying a method and specifying a WDL with an import statement that references a non-existent method" in {
    Post(copyUrl, copyPayload(testBadAgoraEntityNonExistentWdlImportFormat.payload)) ~>
      methodsService.querySingleRoute ~> check {
      assert(status == InternalServerError)
      assert(body.asString contains "Failed to import workflow http://broad.non_existent_grep.1.:\\nbroad.non_existent_grep.1")
    }
  }

  private def copyPayload(wdl:Option[String]) = testAgoraEntity.copy(payload=wdl)

}
