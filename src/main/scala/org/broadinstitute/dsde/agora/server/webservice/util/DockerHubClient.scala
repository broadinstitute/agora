package org.broadinstitute.dsde.agora.server.webservice.util

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import spray.client.pipelining._
import spray.http.HttpResponse
import spray.http.StatusCodes._
import org.broadinstitute.dsde.agora.server.webservice.util.DockerHubJsonSupport._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

case class DockerImageReference(user: Option[String], repo: String, tag: String)

case class DockerTagInfo(pk: Int, id: String)

object DockerHubClient {

  implicit val actorSystem = ActorSystem("agora")

  import actorSystem.dispatcher

  val timeout = 5.seconds

  def pipeline = sendReceive

  def dcGET(url: String): Future[HttpResponse] = pipeline(Get(url))

  def doesDockerImageExist(dockerImage: DockerImageReference): Boolean = {
    val dockerImageInfo = dcGET(dockerImageTagUrl(dockerImage))

    val dockerTagExists = dockerImageInfo map { response: HttpResponse =>
      response.status match {
        case OK => unmarshal[List[DockerTagInfo]].apply(response).nonEmpty
        case NotFound => throw new DockerImageNotFoundException(dockerImage)
        case _ => throw new DockerImageNotFoundException(dockerImage)
      }
    }

    dockerImageInfo.onFailure { case _ => throw DockerImageNotFoundException(dockerImage) }
    val isDockerImageInfoFound = Await.result(dockerTagExists, timeout)
    isDockerImageInfoFound
  }

  val dockerImageRepositoryBaseUrl = "https://index.docker.io/v1/repositories/"
  def dockerImageTagUrl(dockerImage: DockerImageReference) = {
    var url = dockerImageRepositoryBaseUrl
    if (dockerImage.user.isDefined) {
      url += dockerImage.user.get + "/"
    }
    url += dockerImage.repo + "/tags/" + dockerImage.tag
    url
  }

}

