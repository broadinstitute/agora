package org.broadinstitute.dsde.agora.server.webservice.util

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object DockerHubJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val DockerTagInfoFormat: RootJsonFormat[DockerTagInfo] = jsonFormat2(DockerTagInfo)
}
