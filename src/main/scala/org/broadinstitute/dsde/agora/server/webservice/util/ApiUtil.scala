package org.broadinstitute.dsde.agora.server.webservice.util

import org.broadinstitute.dsde.agora.server.AgoraConfig

object ApiUtil {
  val Methods = new ServiceRoute(AgoraConfig.methodsRoute)

  class ServiceRoute(val path: String) {
    def withLeadingSlash: String = {
      "/" + path
    }
  }
}
