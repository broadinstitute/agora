package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient._
import org.broadinstitute.dsde.agora.server.model.AgoraStatus

import scala.util.Failure

class AgoraDBStatus {
  def status(): AgoraStatus = {
    val mongo = getMongoDBStatus
    val sql = sqlDBStatus

    val errors = Seq(mongo, sql).collect {
      case (Failure(t)) => t.getMessage
    }
    AgoraStatus(errors.isEmpty, errors)
  }
}
