
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.vault.common.directives.OpenAMDirectives
import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet
import spray.routing._

import scala.concurrent.ExecutionContext

/**
 * This is just a pass through to vault common.
 */
trait AgoraOpenAMDirectives extends AgoraDirectives {
  def commonNameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    OpenAMDirectives.commonNameFromCookie(magnet)
  }
}
