package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.ActorMaterializer
import org.broadinstitute.dsde.agora.server.AgoraTestData.mockServerPort
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import org.broadinstitute.dsde.agora.server.webservice.util.DockerHubJsonSupport._
import org.broadinstitute.dsde.agora.server.webservice.util.{DockerHubClient, DockerImageReference, DockerTagInfo}
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.scalatest._
import spray.json._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

@DoNotDiscover
class DockerHubClientSpec extends AsyncFreeSpec with Matchers with BeforeAndAfterAll with DockerHubClient {

  override implicit def actorSystem: ActorSystem = ActorSystem("agoraHttpClient")
  override implicit def materializer: ActorMaterializer = ActorMaterializer()
  override implicit def executionContext: ExecutionContext = super[DockerHubClient].executionContext

  var mockServer: ClientAndServer = _
  val validPath = "/repo/tags/tag"
  val emptyPath = "/none/tags/none"
  val notFoundPath = "/not/tags/found"
  val exceptionPath = "/exception/tags/exception"

  val dockerTags = List(
    DockerTagInfo(pk = 1, id = "one"),
    DockerTagInfo(pk = 2, id = "two"),
    DockerTagInfo(pk = 3, id = "three"))

  override protected def beforeAll(): Unit = {
    mockServer = startClientAndServer(mockServerPort)
    mockServer.when(
      request()
        .withMethod("GET")
        .withPath(validPath))
      .respond(org.mockserver.model.HttpResponse.response().
        withBody(dockerTags.toJson.toString()).
        withStatusCode(OK.intValue).
        withHeader("Content-Type", ContentTypes.`application/json`.toString()))

    mockServer.when(
      request()
        .withMethod("GET")
        .withPath(emptyPath))
      .respond(org.mockserver.model.HttpResponse.response().
        withBody(List.empty[DockerTagInfo].toJson.toString).
        withStatusCode(OK.intValue).
        withHeader("Content-Type", ContentTypes.`application/json`.toString()))

    mockServer.when(
      request()
        .withMethod("GET")
        .withPath(notFoundPath))
      .respond(org.mockserver.model.HttpResponse.response().
        withStatusCode(NotFound.intValue).
        withHeader("Content-Type", ContentTypes.`application/json`.toString()))

    mockServer.when(
      request()
        .withMethod("GET")
        .withPath(exceptionPath))
      .respond(org.mockserver.model.HttpResponse.response().
        withStatusCode(InternalServerError.intValue).
        withHeader("Content-Type", ContentTypes.`application/json`.toString()))

  }

  override protected def afterAll(): Unit = {
    if (mockServer.isRunning) { mockServer.stop() }
    Await.result(actorSystem.terminate(), 30.seconds)
  }

  // We need to point all docker requests to local
  override def dockerImageRepositoryBaseUrl = s"http://localhost:$mockServerPort/"

  "DockerHubClient" - {

    "should return that valid docker image references exists" in {
      val dockerImage = DockerImageReference(user = None, repo = "repo", tag = "tag")
      doesDockerImageExist(dockerImage).map { exists =>
        exists shouldBe true
      }
    }

    "should return that no docker image references exist" in {
      val dockerImage = DockerImageReference(user = None, repo = "none", tag = "none")
      doesDockerImageExist(dockerImage).map { exists =>
        exists shouldBe false
      }
    }

    "should throw an exception for Not Found" in {
      val dockerImage = DockerImageReference(user = None, repo = "not", tag = "found")
      recoverToSucceededIf[DockerImageNotFoundException] {
        doesDockerImageExist(dockerImage)
      }
    }

    "should throw an exception for Internal Server Error" in {
      val dockerImage = DockerImageReference(user = None, repo = "exception", tag = "exception")
      recoverToSucceededIf[DockerImageNotFoundException] {
        doesDockerImageExist(dockerImage)
      }
    }
  }

}
