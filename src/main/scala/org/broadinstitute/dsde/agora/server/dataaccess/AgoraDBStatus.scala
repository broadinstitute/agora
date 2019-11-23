package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.{HealthMonitor, SubsystemStatus}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AgoraDBStatus(dataSource: PermissionsDataSource) {

  def mongoStatus()(implicit executionContext: ExecutionContext): Future[SubsystemStatus] = {
    Future(getMongoDBStatus) map {
      case Success(_) => HealthMonitor.OkStatus
      case Failure(t) => HealthMonitor.failedStatus(t.getMessage)
    }
  }

  def mysqlStatus()(implicit executionContext: ExecutionContext): Future[SubsystemStatus] = {
    dataSource.inTransaction { db =>
      db.aePerms.sqlDBStatus().asTry map {
        case Success(_) => HealthMonitor.OkStatus
        case Failure(t) => HealthMonitor.failedStatus(t.getMessage)
      }
    }
  }

}
