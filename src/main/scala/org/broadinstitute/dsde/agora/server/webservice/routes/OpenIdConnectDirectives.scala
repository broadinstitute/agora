package org.broadinstitute.dsde.agora.server.webservice.routes

import spray.routing.Directives._
import spray.routing._
import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet

import scala.concurrent.ExecutionContext


/**
 * Directives class to retrieve user information from the OpenId Connect proxy
 * provided information in the header
 */
class OpenIdConnectDirectives extends AgoraDirectives {

  override def commonNameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    headerValueByName("OIDC_CLAIM_name")
  }

  override def usernameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    headerValueByName("OIDC_CLAIM_email")
  }

}

object OpenIdConnectDirectives extends OpenIdConnectDirectives