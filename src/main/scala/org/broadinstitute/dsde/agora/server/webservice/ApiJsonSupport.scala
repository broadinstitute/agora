package org.broadinstitute.dsde.agora.server.webservice

import spray.httpx.SprayJsonSupport
import spray.json._

object ApiJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val MethodsResponseFormat = jsonFormat3(MethodsQueryResponse)
}
