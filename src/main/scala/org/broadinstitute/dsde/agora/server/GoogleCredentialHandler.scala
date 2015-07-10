package org.broadinstitute.dsde.agora.server

import java.io.File
import java.util.Collections

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.storage.StorageScopes.DEVSTORAGE_FULL_CONTROL

object GoogleCredentialHandler {

  val one_minute = 60

  val credential: GoogleCredential = {
    val emailAddress = AgoraConfig.gcsServiceAccountUserEmail
    val JSON_FACTORY = JacksonFactory.getDefaultInstance
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    new GoogleCredential.Builder()
      .setTransport(httpTransport)
      .setJsonFactory(JSON_FACTORY)
      .setServiceAccountId(emailAddress)
      .setServiceAccountPrivateKeyFromP12File(new File(AgoraConfig.gcServiceAccountP12KeyFile))
      .setServiceAccountScopes(Collections.singleton(DEVSTORAGE_FULL_CONTROL))
      .build()
  }

  def accessToken: String = {
    val expires = Option(credential.getExpiresInSeconds)

    expires match {
      case Some(duration) => if (duration < one_minute) credential.refreshToken()
      case None => credential.refreshToken()
    }
    credential.getAccessToken
  }
}
