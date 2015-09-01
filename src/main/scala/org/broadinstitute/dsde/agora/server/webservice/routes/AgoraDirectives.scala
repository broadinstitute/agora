
package org.broadinstitute.dsde.agora.server.webservice.routes

import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet
import spray.routing._

import scala.concurrent.ExecutionContext

/**
 * AgoraDirectives trait allows us to specify directives that may be implemented either by specific implementation
 * classes such as the OpenAM directives class in vault common, the OpenIdConnect directives class, or the mock OpenAM
 * directives in our test suite. Using the mock directives we can unit test without having to integration test with a
 * running OpenAM server.
 */
trait AgoraDirectives {
  def commonNameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String]

  def usernameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String]
}
