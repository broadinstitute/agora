package org.broadinstitute.dsde.agora.server.webservice.routes

import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import org.broadinstitute.dsde.agora.server.dataaccess.SamClient
import org.broadinstitute.dsde.agora.server.webservice.util.ImplicitMagnet

import scala.concurrent.ExecutionContext


/**
 * Directives class to retrieve user information from the OpenId Connect proxy
 * provided information in the header.
 */
class OpenIdConnectDirectives extends AgoraDirectives {
  private val serviceAccountDomain = "\\S+@\\S+\\.iam\\.gserviceaccount\\.com".r

  private def isServiceAccount(email: String) = {
    serviceAccountDomain.pattern.matcher(email).matches
  }

  override def usernameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    (headerValueByName("OIDC_CLAIM_email") & headerValueByName("OIDC_access_token")).tflatMap { case (email, token) =>
      if (isServiceAccount(email)) {
        onSuccess(SamClient.getUserEmail(token)).map(_.getOrElse(email))
      } else {
        provide(email)
      }
    }
  }
}

object OpenIdConnectDirectives extends OpenIdConnectDirectives