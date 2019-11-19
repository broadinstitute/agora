package org.broadinstitute.dsde.agora.server.dataaccess

import cromwell.client.ApiClient
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

  def describe(payload: String, accessToken: String) :Try[WorkflowDescription] =
    Try(womtoolApi(accessToken).describe("v1", payload, null, null, null, null))

  def validate(payload: String, accessToken: String): Try[String] = {
    describe(payload, accessToken) match {
      case Success(wd) if wd.getValidWorkflow => Success("OK")
      case Success(wd) =>
        Failure(ValidationException(String.join("\n", wd.getErrors)))
      case Failure(ex) =>
        Failure(ex)
    }
  }
}
