package org.broadinstitute.dsde.agora.server.dataaccess

import cromwell.client.{ApiClient, ApiException}
import cromwell.client.api.WomtoolApi
import cromwell.client.model.WorkflowDescription
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import scala.util.{Failure, Success, Try}


object WaasClient {
  private def womtoolApi(accessToken: String) = {
    val apiClient = new ApiClient()
    apiClient.setAccessToken(accessToken)
    apiClient.setBasePath(AgoraConfig.waasServer.get)

    // Really useful to see what HTTP requests/responses calls are being made...
    // apiClient.setDebugging(true)

    new WomtoolApi(apiClient)
  }

  def validate(payload: String, accessToken: String): Try[String] = {
    // is there a cleaner way to do this?
    try {
      val wd = womtoolApi(accessToken).describe("v1", payload, null, null, null, null)

      if (wd.getValidWorkflow) {
        Success("OK")
      } else {
        Failure(ValidationException(String.join("\n", wd.getErrors())))
      }
    } catch {
      case e: ApiException => Failure(e)
    }
  }

  def describe(wdl: String, accessToken: String) :Try[WorkflowDescription] = {
    Try {
      womtoolApi(accessToken).describe("v1", wdl, null, null, null, null)
    } recoverWith {
      case e: ApiException => Failure(e)
    }
  }

}
