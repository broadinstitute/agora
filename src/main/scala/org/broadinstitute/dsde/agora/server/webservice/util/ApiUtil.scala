package org.broadinstitute.dsde.agora.server.webservice.util

object ApiUtil {
  val Tasks: ServiceRoute = new ServiceRoute("tasks")

  class ServiceRoute(val path: String) {
    def withLeadingSlash: String = {
      "/" + path
    }
  }

}
