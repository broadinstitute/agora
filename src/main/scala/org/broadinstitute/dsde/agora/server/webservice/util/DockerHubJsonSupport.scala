package org.broadinstitute.dsde.agora.server.webservice.util

import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object DockerHubJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val DockerTagInfoFormat = jsonFormat2(DockerTagInfo)
}
