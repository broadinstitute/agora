package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.SwaggerRoutes
import org.scalatest.DoNotDiscover
import org.scalatest.flatspec.AnyFlatSpecLike

@DoNotDiscover
class SwaggerRoutesSpec extends ApiServiceSpec with AnyFlatSpecLike with SwaggerRoutes  {

  // these are defined as exact-match
  Seq("/agora.yaml") foreach { path =>
    s"Swagger exact-match routing for $path" should "be handled by SwaggerRoutes" in {
      Get(path) ~> swaggerRoutes ~> check {
        assert(handled)
      }
    }
  }

  // for these tests, make sure the path being tested exists in META-INF/resources/webjars/swagger-ui/*,
  // otherwise the route won't be handled!
  Seq("/oauth2-redirect.html", "/swagger-ui.js") foreach { path =>
    s"Swagger path-prefix routing for $path" should "be handled by SwaggerRoutes" in {
      Get(path) ~> swaggerRoutes ~> check {
        assert(handled)
      }
    }
  }
}
