package org.broadinstitute.dsde.agora.server.webservice.routes

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.webservice.util.ImplicitMagnet
import spray.routing.Directives._
import spray.routing._

import scala.concurrent.ExecutionContext

trait MockAgoraDirectives extends AgoraDirectives {
  def commonNameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    provide(AgoraConfig.mockAuthenticatedUserEmail)
  }

  // allow unit tests to specify the user making the request; if they don't, use the default
  def usernameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    optionalHeaderValueByName(MockAgoraDirectives.mockAuthenticatedUserEmailHeader) map {
      case Some(mockAuthenticatedUserEmail) => mockAuthenticatedUserEmail
      case None => AgoraConfig.mockAuthenticatedUserEmail
    }
  }
}

object MockAgoraDirectives extends MockAgoraDirectives {
  val mockAuthenticatedUserEmailHeader = "X-UnitTest-MockAuthenticatedUserEmail"
}
