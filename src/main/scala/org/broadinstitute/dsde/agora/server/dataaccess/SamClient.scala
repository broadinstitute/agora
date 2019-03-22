package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi
import org.broadinstitute.dsde.workbench.client.sam.{ApiClient, ApiException}

import scala.util.{Failure, Success, Try}

object SamClient {
  private def samUserApi(accessToken: String) = {
    val apiClient = new ApiClient()
    apiClient.setAccessToken(accessToken)
    apiClient.setBasePath(AgoraConfig.samUrl)
    new UsersApi(apiClient)
  }

  def getUserEmail(token: String): Option[String] = {
    val api = samUserApi(token)
    Try(api.getUserStatusInfo()) match {
      case Success(userInfo) => Option(userInfo.getUserEmail)
      case Failure(e: ApiException) if e.getCode == 404 => None
      case Failure(regrets) => throw regrets
    }
  }
}
