package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.model.AgoraStatus

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class AgoraDBStatus(dataSource: PermissionsDataSource)(implicit ec: ExecutionContext) {
  def status(): Future[AgoraStatus] = {
    val mongo = getMongoDBStatus

    dataSource.inTransaction { db =>
      db.aePerms.sqlDBStatus()
    } map { sql =>
      val errors = Seq(mongo, sql).collect {
        case (Failure(t)) => t.getMessage
      }
      AgoraStatus(errors.isEmpty, errors)
    }
  }
}
