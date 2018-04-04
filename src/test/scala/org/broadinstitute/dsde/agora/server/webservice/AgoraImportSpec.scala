
package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

import org.scalatest.{DoNotDiscover, FlatSpecLike}
import org.scalatest.Matchers._

import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer._
import org.mockserver.model.HttpRequest.request

@DoNotDiscover
class AgoraImportSpec extends ApiServiceSpec with FlatSpecLike{
  var testAgoraEntityWithId: AgoraEntity = _

  var mockServer: ClientAndServer = _

  val routes = ApiService.handleExceptionsAndRejections {
    methodsService.querySingleRoute ~ methodsService.postRoute
  }

  private val errorMessagePrefix = "Invalid WDL:"

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    testAgoraEntityWithId = patiently(agoraBusiness.insert(testAgoraEntity, mockAuthenticatedOwner.get))

    mockServer = startClientAndServer(mockServerPort)

    mockServer.when(
      request()
      .withMethod("GET")
      .withPath(testGA4GHpath))
      .respond(org.mockserver.model.HttpResponse.response().withBody(payload1.get).withStatusCode(OK.intValue).withHeader("Content-Type", "text/plain"))
  }

  override def afterAll() = {
    clearDatabases()
    mockServer.stop()
  }

  // tests for creating new methods
  "MethodsService" should "return 400 when posting a gibberish WDL" in {
    Post(ApiUtil.Methods.withLeadingVersion, copyPayload(Some("this isn't valid WDL!"))) ~>
      routes ~> check {
      assert(status == BadRequest)
      assert(responseAs[String] contains s"$errorMessagePrefix Unrecognized token")
    }
  }

  "MethodsService" should "return a 400 when posting a WDL with an invalid import statement" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityInvalidWdlImportFormat) ~>
      routes ~> check {
      assert(status == BadRequest)
      assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow invalid_syntax_for_tool.:")
    }
  }

  "MethodsService" should "return a 400 when posting a WDL with an import statement that references a non-existent method" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityNonExistentWdlImportFormat) ~>
      routes ~> check {
      assert(status == BadRequest)
      assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow broad.non_existent_grep.1.:")
    }
  }

  "MethodsService" should "return a 400 when posting a method and specifying a WDL with an import URL that returns a 404" in {
    Post(ApiUtil.Methods.withLeadingVersion, copyPayload(testBadAgoraEntityWdlImportNotFound.payload)) ~>
      routes ~> check {
      assert(status == BadRequest)
      assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow http://localhost:$mockServerPort/not-found")
    }
  }

  "MethodsService" should "return 201 when posting a valid WDL that contains an import" in {
    Post(ApiUtil.Methods.withLeadingVersion, testEntityWorkflowWithExistentWdlImport) ~>
      routes ~> check {
      assert(status == Created)
      assert(responseAs[String] contains "\"name\":\"testMethod1\"")
      assert(responseAs[String] contains "\"createDate\"")
    }
  }

  // tests for copying a method and specifying new WDL for the copy

  private val copyUrl = ApiUtil.Methods.withLeadingVersion +
    s"/${testAgoraEntity.namespace.get}/${testAgoraEntity.name.get}/1"


  "MethodsService" should "return 400 when copying a method and specifying a gibberish WDL" in {
    Post(copyUrl, copyPayload(Some("this isn't valid WDL!"))) ~>
      routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Unrecognized token")
      }
  }

  "MethodsService" should "return a 400 when copying a method and specifying a WDL with an invalid import statement" in {
    Post(copyUrl, copyPayload(testBadAgoraEntityInvalidWdlImportFormat.payload)) ~>
      routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow invalid_syntax_for_tool.:")
      }
  }

  "MethodsService" should "return a 400 when copying a method and specifying a WDL with an import statement that references a non-existent method" in {
    Post(copyUrl, copyPayload(testBadAgoraEntityNonExistentWdlImportFormat.payload)) ~>
      routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow broad.non_existent_grep.1.:")
      }
  }

  "MethodsService" should "return a 400 when copying a method and specifying a WDL with an import URL that returns a 404" in {
    Post(copyUrl, copyPayload(testBadAgoraEntityWdlImportNotFound.payload)) ~>
      routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow http://localhost:$mockServerPort/not-found")
      }
  }

  "MethodsService" should "return 200 when copying a method and specifying a valid WDL that contains an import" in {
    Post(copyUrl, copyPayload(testEntityWorkflowWithExistentWdlImport.payload)) ~>
      routes ~> check {
        assert(status == OK)
        assert(responseAs[String] contains "\"name\":\"testMethod1\"")
        assert(responseAs[String] contains "\"createDate\"")
      }
  }

  private def copyPayload(wdl:Option[String]) = testAgoraEntity.copy(payload=wdl)

}
