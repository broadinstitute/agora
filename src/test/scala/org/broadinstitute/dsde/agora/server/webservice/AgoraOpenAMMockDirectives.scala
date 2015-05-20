
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.vault.common.openam.OpenAMResponse.UsernameCNResponse
import org.broadinstitute.dsde.vault.common.util.ImplicitMagnet
import spray.routing.Directives._
import spray.routing._

import scala.concurrent.{ExecutionContext, Future}

/**
 * This is a mock of OpenAM vault common directives.
 */
trait AgoraOpenAMMockDirectives extends AgoraDirectives {
  def commonNameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    implicit val ec = magnet.value
    val userNameFuture = for {
      usernameCN <- Future(new UsernameCNResponse("agora-test", Seq("agora-test")))
    } yield usernameCN.cn.head
    onSuccess(userNameFuture)
  }

  def usernameFromCookie(magnet: ImplicitMagnet[ExecutionContext]): Directive1[String] = {
    implicit val ec = magnet.value
    val userNameFuture = for {
      usernameCN <- Future(new UsernameCNResponse("agora-test", Seq("agora-test")))
    } yield usernameCN.username
    onSuccess(userNameFuture)
  }

}
