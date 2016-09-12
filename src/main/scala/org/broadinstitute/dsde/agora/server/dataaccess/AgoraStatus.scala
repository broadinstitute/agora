package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient._

class AgoraStatus {
  def status(): (Boolean, String) = {
    val (up, message) = getMongoDBStatus
    try {
      // sql up, mongo may be up or down
      sqlDBStatus
      return (up, s"""{"status": "down", "mongo error": "$message" }""")
    }
    catch {
      // sql down, mongo may be up or down
      case e: Throwable => {
        if (up) (false, s"""{"status": "down", "sql error": "${e.getMessage}" }""")
        else (false, s"""{"status": "down", "mongo error": "$message", "sql error": "${e.getMessage}" }""")
      }
    }
  }
}
