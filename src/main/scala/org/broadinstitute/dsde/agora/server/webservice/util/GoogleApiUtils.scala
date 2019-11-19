package org.broadinstitute.dsde.agora.server.webservice.util

import java.io.File
import java.util.Collections

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.admin.directory.{DirectoryScopes, Directory}
import org.broadinstitute.dsde.agora.server.AgoraConfig


object GoogleApiUtils {
  val emailAddress = AgoraConfig.gcsServiceAccountEmail
  val JSON_FACTORY = JacksonFactory.getDefaultInstance
  val httpTransport = GoogleNetHttpTransport.newTrustedTransport
  val directoryScopes = Collections.singleton(DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER_READONLY)

  private def getGroupServiceAccountCredential: Credential = {
    new GoogleCredential.Builder()
      .setTransport(httpTransport)
      .setJsonFactory(JSON_FACTORY)
      .setServiceAccountId(emailAddress)
      .setServiceAccountScopes(directoryScopes)
      .setServiceAccountUser(AgoraConfig.gcsUserEmail)
      .setServiceAccountPrivateKeyFromPemFile(new File(AgoraConfig.gcsServiceAccountPemFile))
      .build()
  }

  def getGroupDirectory = {
    new Directory.Builder(httpTransport, JSON_FACTORY, getGroupServiceAccountCredential).setApplicationName("firecloud:agora").build()
  }
}
