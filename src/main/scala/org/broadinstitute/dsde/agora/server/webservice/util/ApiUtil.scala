package org.broadinstitute.dsde.agora.server.webservice.util

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import net.ceedubs.ficus.Ficus._

object ApiUtil {
  val Methods: ServiceRoute = new ServiceRoute("methods")

  val scheme = AgoraConfig.appConfig.as[Option[String]]("webservice.scheme").getOrElse("http")
  val host = AgoraConfig.appConfig.as[Option[String]]("webservice.host").getOrElse("localhost")
  val port = AgoraConfig.appConfig.as[Option[Int]]("webservice.port").getOrElse(8000)
  val baseUrl = scheme + "://" + host + ":" + port
  val methodsUrl = baseUrl + Methods.withLeadingSlash + "/"

  class ServiceRoute(val path: String) {
    def withLeadingSlash: String = {
      "/" + path
    }
  }

  def agoraUrl(entity: AgoraEntity): String = {
    methodsUrl + entity.namespace.get + "/" + entity.name.get + "/" + entity.snapshotId.get
  }

}
