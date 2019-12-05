package org.broadinstitute.dsde.agora.server.dataaccess.health

import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.workbench.util.health.Subsystems.{Database, Mongo}

/**
 * Utility method for converting the AgoraDBStatus to a HealthMonitorSubsystems used by the TypedHealthMonitor.
 */
object AgoraHealthMonitorSubsystems {
  implicit class EnhancedAgoraDBStatus(val dbStatus: AgoraDBStatus) extends AnyVal {
    def toHealthMonitorSubsystems: HealthMonitorSubsystems = {
      new HealthMonitorSubsystems(Map(
        Database -> (executionContext => dbStatus.mysqlStatus()(executionContext)),
        Mongo -> (executionContext => dbStatus.mongoStatus()(executionContext))
      ))
    }
  }
}
