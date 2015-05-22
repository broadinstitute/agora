
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.webservice.util.AgoraOpenAMClient
import org.broadinstitute.dsde.vault.common.directives.OpenAMDirectives
import org.broadinstitute.dsde.vault.common.directives.OpenAMDirectives._
import org.broadinstitute.dsde.vault.common.openam.{OpenAMClient, OpenAMConfig}
import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet
import spray.routing.Directives._
import spray.routing._

import scala.concurrent.ExecutionContext

/**
 * This is just a pass through to vault common.
 */
trait AgoraOpenAMDirectives extends AgoraDirectives {

  def commonNameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    OpenAMDirectives.commonNameFromCookie(magnet)
  }

  def usernameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    implicit val ec = magnet.value
    tokenFromCookie flatMap usernameFromToken
  }

  def usernameFromToken(token: String)(implicit ec: ExecutionContext): Directive1[String] = {
    val userInfoFuture = for {
      id <- OpenAMClient.lookupIdFromSession(OpenAMConfig.deploymentUri, token)
      userInfo <- AgoraOpenAMClient.lookupUserInfo(OpenAMConfig.deploymentUri, token, id.id, id.realm)
    } yield userInfo.mail.head
    onSuccess(userInfoFuture)
  }

}
