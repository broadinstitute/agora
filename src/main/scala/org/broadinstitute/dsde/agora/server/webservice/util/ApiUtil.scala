package org.broadinstitute.dsde.agora.server.webservice.util

object ApiUtil {
  val Methods: ServiceRoute = new ServiceRoute("methods")

  class ServiceRoute(val path: String) {
    def withLeadingSlash: String = {
      "/methods/" + path
    }
  }

}
