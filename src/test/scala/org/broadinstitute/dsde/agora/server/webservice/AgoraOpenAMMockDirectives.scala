
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.webservice.routes.AgoraDirectives
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet
import spray.routing.Directives._
import spray.routing._

import scala.concurrent.ExecutionContext

/**
 * This is a mock of OpenAM vault common directives.
 */
trait AgoraOpenAMMockDirectives extends AgoraDirectives {
  def commonNameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    provide("broadprometheustest@gmail.com")
  }

  def usernameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    provide("broadprometheustest@gmail.com")
  }

}
