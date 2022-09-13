package org.broadinstitute.dsde.agora.server.webservice.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import org.broadinstitute.dsde.agora.server.dataaccess.SamClient
import org.broadinstitute.dsde.agora.server.exceptions.AgoraException
import org.broadinstitute.dsde.agora.server.webservice.util.ImplicitMagnet

import scala.concurrent.ExecutionContext


/**
 * Directives class to retrieve user information from the OpenId Connect proxy
 * provided information in the header.
 */
class OpenIdConnectDirectives extends AgoraDirectives {
  override def usernameFromRequest(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    tokenFromRequest().flatMap { token =>
      onSuccess(SamClient.getSamUser(token)).flatMap {
        case None => failWith(AgoraException("User is not registered", StatusCodes.Unauthorized))
        case Some(userStatusInfo) =>
          if (userStatusInfo.getEnabled) {
            provide(userStatusInfo.getUserEmail)
          } else {
            failWith(AgoraException("User is disabled", StatusCodes.Unauthorized))
          }
      }
    }
  }

  override def tokenFromRequest() = headerValueByName("OIDC_access_token")

}

object OpenIdConnectDirectives extends OpenIdConnectDirectives