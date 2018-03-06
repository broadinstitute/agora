package org.broadinstitute.dsde.agora.server.webservice.util

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller
import org.broadinstitute.dsde.agora.server.exceptions.DockerImageNotFoundException
import org.broadinstitute.dsde.agora.server.webservice.util.DockerHubJsonSupport._
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration._

case class DockerImageReference(user: Option[String], repo: String, tag: String)

case class DockerTagInfo(pk: Int, id: String)

trait DockerHubClient extends HttpClient {

  val timeout = 20.seconds

  def dcGET(url: String): Future[HttpResponse] = get(Uri(url))

  def doesDockerImageExist(dockerImage: DockerImageReference): Future[Boolean] = {
    val dockerImageInfo: Future[HttpResponse] = dcGET(dockerImageTagUrl(dockerImage))

    dockerImageInfo flatMap { response: HttpResponse => //on success
        response.status match {
          case StatusCodes.OK => unmarshal(response) map { l => l.nonEmpty }
          case StatusCodes.NotFound => throw DockerImageNotFoundException(dockerImage)
          case _ => throw DockerImageNotFoundException(dockerImage)
        }
      } recoverWith { //on failure
        case e => throw DockerImageNotFoundException(dockerImage)
      }
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

  private def unmarshal(response: HttpResponse): Future[List[DockerTagInfo]] = {
    Unmarshaller.
      stringUnmarshaller.
      forContentTypes(ContentTypes.`application/json`).
      map(_.parseJson.convertTo[List[DockerTagInfo]]).
      apply(response.entity)
  }

}

