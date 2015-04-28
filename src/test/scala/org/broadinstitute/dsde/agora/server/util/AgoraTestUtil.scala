package org.broadinstitute.dsde.agora.server.util

import org.broadinstitute.dsde.agora.server.webservice.TasksQueryResponse

object AgoraTestUtil {
  val files: Map[String, String] = Map("cwlFile" -> "fake/file/url")

  val taskMetadataMap = Map(
    "author" -> "Dave Shiga", 
    "version" -> "1.0"
  )

  val testVaultId = "EAC779AD3F"

  def testTask(vaultId: String) = {
    TasksQueryResponse(
      vaultId,
      files,
      taskMetadataMap
    )
  }
}
