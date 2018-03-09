package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.SwaggerRoutes
import org.scalatest.{DoNotDiscover, FlatSpecLike}

@DoNotDiscover
class SwaggerRoutesSpec extends ApiServiceSpec with FlatSpecLike with SwaggerRoutes  {

  // these are defined as exact-match
  Seq("/agora.yaml", "/o2c.html", "/swagger-ui.js") foreach { path =>
    s"Swagger exact-match routing for $path" should "be handled by SwaggerRoutes" in {
      Get(path) ~> swaggerRoutes ~> check {
        assert(handled)
      }
    }
  }

  // for these tests, make sure the path being tested exists in META-INF/resources/webjars/swagger-ui/2.2.10-1,
  // otherwise the route won't be handled!
  Seq("/images/logo_small.png", "/lib/marked.js", "/css/reset.css") foreach { path =>
    s"Swagger path-prefix routing for $path" should "be handled by SwaggerRoutes" in {
      Get(path) ~> swaggerRoutes ~> check {
        assert(handled)
      }
    }
  }
}
