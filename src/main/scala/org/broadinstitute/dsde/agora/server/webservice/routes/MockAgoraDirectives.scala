package org.broadinstitute.dsde.agora.server.webservice.routes

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.webservice.util.ImplicitMagnet

import scala.concurrent.ExecutionContext

trait MockAgoraDirectives extends AgoraDirectives {
  // allow unit tests to specify the user making the request; if they don't, use the default
  def usernameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    optionalHeaderValueByName(MockAgoraDirectives.mockAuthenticatedUserEmailHeader) map {
      case Some(mockAuthenticatedUserEmail) => mockAuthenticatedUserEmail
      case None => AgoraConfig.mockAuthenticatedUserEmail
    }
  }

  override def tokenFromRequest = headerValueByName(MockAgoraDirectives.mockAccessToken)
}

object MockAgoraDirectives extends MockAgoraDirectives {
  val mockAuthenticatedUserEmailHeader = "X-UnitTest-MockAuthenticatedUserEmail"
  val mockAccessToken = "X-UnitTestMockAccessToken"
}
