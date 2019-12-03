package org.broadinstitute.dsde.agora.server.dataaccess

import com.typesafe.scalalogging.StrictLogging
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.{HealthMonitor, SubsystemStatus}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AgoraDBStatus(dataSource: PermissionsDataSource) extends StrictLogging {

  def mongoStatus()(implicit executionContext: ExecutionContext): Future[SubsystemStatus] = {
    for {
      _ <- Future(logger.info("Retrieving Mongo status"))
      status <- Future(getMongoDBStatus) map {
        case Success(_) => HealthMonitor.OkStatus
        case Failure(t) => HealthMonitor.failedStatus(t.getMessage)
      }
      _ <- Future(logger.info(s"Retrieved Mongo status: $status"))
    } yield status
  }

  def mysqlStatus()(implicit executionContext: ExecutionContext): Future[SubsystemStatus] = {
    for {
      _ <- Future(logger.info("Retrieving MySQL status"))
      status <- dataSource.inTransaction { db =>
        db.aePerms.sqlDBStatus().asTry map {
          case Success(_) => HealthMonitor.OkStatus
          case Failure(t) => HealthMonitor.failedStatus(t.getMessage)
        }
      }
      _ <- Future(logger.info(s"Retrieved MySQL status: $status"))
    } yield status
  }

}
