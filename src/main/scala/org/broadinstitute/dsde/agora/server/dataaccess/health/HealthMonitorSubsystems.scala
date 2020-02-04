package org.broadinstitute.dsde.agora.server.dataaccess.health

import org.broadinstitute.dsde.workbench.util.health.SubsystemStatus
import org.broadinstitute.dsde.workbench.util.health.Subsystems.Subsystem

import scala.concurrent.{ExecutionContext, Future}

/**
 * Provides the subsystems for the TypedHealthMonitor to run its status checks.
 */
class HealthMonitorSubsystems(subsystemsToHealthCheck: Map[Subsystem, ExecutionContext => Future[SubsystemStatus]]) {
  def subsystems: Set[Subsystem] = subsystemsToHealthCheck.keySet

  def checkHealth()(implicit executionContext: ExecutionContext): Map[Subsystem, Future[SubsystemStatus]] = {
    subsystemsToHealthCheck.mapValues(_(executionContext))
  }
}
