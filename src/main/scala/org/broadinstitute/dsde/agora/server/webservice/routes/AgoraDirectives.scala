
package org.broadinstitute.dsde.agora.server.webservice.routes

import akka.http.scaladsl.server.Directive1
import org.broadinstitute.dsde.agora.server.webservice.util.ImplicitMagnet

import scala.concurrent.ExecutionContext

/**
 * AgoraDirectives trait allows us to specify directives that may be implemented either by specific implementation
 * classes such as the OpenIdConnect directives class, or the mock
 * directives in our test suite. Using the mock directives we can unit test without having to integration test with a
 * running server.
 */
trait AgoraDirectives {
  def commonNameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String]

  def usernameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String]
}
