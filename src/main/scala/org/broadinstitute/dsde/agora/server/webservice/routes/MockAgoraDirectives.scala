package org.broadinstitute.dsde.agora.server.webservice.routes

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.vault.common.directives.OpenAMDirectives
import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet
import spray.routing.Directives._
import spray.routing._

import scala.concurrent.ExecutionContext

trait MockAgoraDirectives extends AgoraDirectives {
  def commonNameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    provide(AgoraConfig.mockAuthenticatedUserEmail.split("@")(0))
  }

  def usernameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    provide(AgoraConfig.mockAuthenticatedUserEmail)
  }
}
