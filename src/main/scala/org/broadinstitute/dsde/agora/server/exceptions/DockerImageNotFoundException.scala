package org.broadinstitute.dsde.agora.server.exceptions

import org.broadinstitute.dsde.agora.server.webservice.util.DockerImageReference

case class DockerImageNotFoundException(dockerImage: DockerImageReference) extends Exception {
  override def getMessage: String = {
    s"Docker Image for User ${dockerImage.user.getOrElse("'Official'")}/" + s"${dockerImage.repo}" + s":${dockerImage.tag} not found."
  }
}