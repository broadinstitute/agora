package org.broadinstitute.dsde.agora.server.webservice.util

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import spray.client.pipelining._
import spray.http.{HttpResponse, StatusCodes}
import org.broadinstitute.dsde.agora.server.webservice.util.DockerHubJsonSupport._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

case class DockerImageReference(user: Option[String], repo: String, tag: String)

case class DockerTagInfo(pk: Int, id: String)

object DockerHubClient {

  implicit val actorSystem = ActorSystem("agora")

  import actorSystem.dispatcher

  val timeout = 20.seconds

  def pipeline = sendReceive

  def dcGET(url: String): Future[HttpResponse] = pipeline(Get(url))

  def doesDockerImageExist(dockerImage: DockerImageReference): Future[Boolean] = {
    val dockerImageInfo: Future[HttpResponse] = dcGET(dockerImageTagUrl(dockerImage))

    dockerImageInfo transform(
      { response: HttpResponse => //on success
        response.status match {
          case StatusCodes.OK => unmarshal[List[DockerTagInfo]].apply(response).nonEmpty
          case StatusCodes.NotFound => throw DockerImageNotFoundException(dockerImage)
          case _ => throw DockerImageNotFoundException(dockerImage)
        }
      }, { exc => //on failure
        throw DockerImageNotFoundException(dockerImage)
      }
    )
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

