
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

  private val createUrl = ApiUtil.Methods.withLeadingVersion
  private val copyUrl = createUrl + s"/${testAgoraEntity.namespace.get}/${testAgoraEntity.name.get}/1"

  private val operationsByUrls = Map(
    "posting" -> createUrl,
    "copying" -> copyUrl)

  // Positive Tests
  "MethodsService" should s"return 201 when posting a valid WDL that contains an import" in {
    Post(createUrl, testEntityWorkflowWithExistentWdlImport) ~>
      routes ~> check {
      assert(status == Created)
      assert(responseAs[String] contains "\"name\":\"testMethod1\"")
      assert(responseAs[String] contains "\"createDate\"")
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

  // Negative Tests
  operationsByUrls foreach { case (processing, url) =>
    "MethodsService" should s"return 400 upon $processing a gibberish WDL" in {
      Post(url, copyPayload(Some("this isn't valid WDL!"))) ~>
        routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Unrecognized token")
      }
    }

    "MethodsService" should s"return a 400 upon $processing a WDL with an invalid import statement" in {
      Post(url, testBadAgoraEntityInvalidWdlImportFormat) ~>
        routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow invalid_syntax_for_tool.:")
      }
    }

    "MethodsService" should s"return a 400 upon $processing a WDL with an import statement that references a non-existent method" in {
      Post(url, testBadAgoraEntityNonExistentWdlImportFormat) ~>
        routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow broad.non_existent_grep.1.:")
      }
    }

    "MethodsService" should s"return a 400 upon $processing a WDL with an import URL that returns a 404" in {
      Post(url, copyPayload(testBadAgoraEntityWdlImportNotFound.payload)) ~>
        routes ~> check {
        assert(status == BadRequest)
        assert(responseAs[String] contains s"$errorMessagePrefix Failed to import workflow http://localhost:$mockServerPort/not-found")
      }
    }
  }

  private def copyPayload(wdl:Option[String]) = testAgoraEntity.copy(payload=wdl)

}
