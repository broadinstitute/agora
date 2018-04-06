package org.broadinstitute.dsde.agora.server.webservice.util

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import org.broadinstitute.dsde.agora.server.webservice.util.DockerHubJsonSupport._

import scala.concurrent.Future

case class DockerImageReference(user: Option[String], repo: String, tag: String)

case class DockerTagInfo(pk: Int, id: String)

trait DockerHubClient extends HttpClient {

  def dcGET(url: String): Future[HttpResponse] = get(Uri(url))

  def doesDockerImageExist(dockerImage: DockerImageReference): Future[Boolean] = {
    val dockerImageInfo: Future[HttpResponse] = dcGET(dockerImageTagUrl(dockerImage))

    dockerImageInfo flatMap { response: HttpResponse => //on success
        response.status match {
          case StatusCodes.OK => Unmarshal(response.entity).to[List[DockerTagInfo]] map (_.nonEmpty)
          case StatusCodes.NotFound => throw DockerImageNotFoundException(dockerImage)
          case _ => throw DockerImageNotFoundException(dockerImage)
        }
      } recoverWith { //on failure
        case e => throw DockerImageNotFoundException(dockerImage)
      }
  }

  def dockerImageRepositoryBaseUrl = "https://index.docker.io/v1/repositories/"
  def dockerImageTagUrl(dockerImage: DockerImageReference) = {
    var url = dockerImageRepositoryBaseUrl
    if (dockerImage.user.isDefined) {
      url += dockerImage.user.get + "/"
    }
    url += dockerImage.repo + "/tags/" + dockerImage.tag
    url
  }

}

