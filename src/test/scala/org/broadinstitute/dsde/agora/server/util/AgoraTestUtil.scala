package org.broadinstitute.dsde.agora.server.util

import org.broadinstitute.dsde.agora.server.webservice.MethodsQueryResponse

object AgoraTestUtil {
  val files: Map[String, String] = Map("cwlFile" -> "fake/file/url")

  val methodsMetadataMap = Map(
    "author" -> "Dave Shiga", 
    "version" -> "1.0"
  )

  val testVaultId = "EAC779AD3F"

  def testMethod(vaultId: String) = {
    MethodsQueryResponse(
      vaultId,
      files,
      methodsMetadataMap
    )
  }
}
