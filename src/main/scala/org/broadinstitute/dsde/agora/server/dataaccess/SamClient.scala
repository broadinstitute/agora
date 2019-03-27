package org.broadinstitute.dsde.agora.server.dataaccess

import java.util

import akka.http.scaladsl.model.StatusCodes
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo
import org.broadinstitute.dsde.workbench.client.sam.{ApiCallback, ApiClient, ApiException}

import scala.concurrent.{Future, Promise}

object SamClient {
  private def samUserApi(accessToken: String) = {
    val apiClient = new ApiClient()
    apiClient.setAccessToken(accessToken)
    apiClient.setBasePath(AgoraConfig.samUrl)
    new UsersApi(apiClient)
  }

  def getUserEmail(token: String): Future[Option[String]] = {
    val api = samUserApi(token)
    val promise = Promise[Option[String]]()
    api.getUserStatusInfoAsync(new ApiCallback[UserStatusInfo] {
      override def onFailure(e: ApiException, statusCode: Int, responseHeaders: util.Map[String, util.List[String]]): Unit = {
        if (statusCode == StatusCodes.NotFound.intValue) {
          promise.success(None)
        } else {
          promise.failure(e)
        }
      }

      override def onSuccess(result: UserStatusInfo, statusCode: Int, responseHeaders: util.Map[String, util.List[String]]): Unit = {
        promise.success(Option(result.getUserEmail))
      }

      override def onUploadProgress(bytesWritten: Long, contentLength: Long, done: Boolean): Unit = ()

      override def onDownloadProgress(bytesRead: Long, contentLength: Long, done: Boolean): Unit = ()
    })

    promise.future
  }
}
