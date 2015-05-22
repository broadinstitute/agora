package org.broadinstitute.dsde.agora.server.webservice.util

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.vault.common.openam.OpenAMConfig
import spray.client.pipelining._
import spray.http.Uri
import spray.httpx.SprayJsonSupport._

import scala.concurrent.Future

object AgoraOpenAMClient {
  implicit val system = ActorSystem()

  import system.dispatcher

  case class UserInfoResponse(username: String, cn: Seq[String], mail: Seq[String])

  def lookupUserInfo(deploymentUri: String, token: String, id: String, realm: Option[String]): Future[UserInfoResponse] = {
    val uri = Uri(s"$deploymentUri/json${realm.getOrElse("")}/users/$id").
      withQuery("_fields" -> "username,cn,mail")
    val pipeline =
      addToken(token) ~>
        sendReceive ~>
        unmarshal[UserInfoResponse]
    pipeline(Get(uri))
  }

  private def addToken(token: String) =
    addHeader(OpenAMConfig.tokenCookie, token)
}
