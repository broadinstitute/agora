package org.broadinstitute.dsde.agora.server.webservice.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object DockerHubJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val DockerTagInfoFormat = jsonFormat2(DockerTagInfo)
}
