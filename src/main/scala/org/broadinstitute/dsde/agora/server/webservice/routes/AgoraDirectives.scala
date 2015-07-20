
package org.broadinstitute.dsde.agora.server.webservice.routes

import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet
import spray.routing._

import scala.concurrent.ExecutionContext

/**
 * AgoraDirectives trait allows us to specify directives that may be implemented either by the OpenAM directives class
 * in vault common or the mock OpeAM directives in our test suite. Using the mock directives we can unit test without
 * having to integration test with a running OpenAM server.
 */
trait AgoraDirectives {
  def commonNameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String]

  def usernameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String]
}
