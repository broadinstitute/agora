package org.broadinstitute.dsde.agora.server.model

import org.bson.types.ObjectId

/**
 * Caches a collection of methods and config snapshots retrieved at the same time, providing a utility field to return
 * the count of config snapshots per method object id.
 */
case class MethodsAndConfigs(methodSnapshots: Seq[AgoraEntity], configSnapshots: Seq[AgoraEntity]) {
  lazy val configCounts: Map[Option[ObjectId], Int] = {
    configSnapshots.groupBy(_.methodId) map {
      case (objectIdOption, agoraEntities) => objectIdOption -> agoraEntities.size
    }
  }
}

object MethodsAndConfigs {
  def from(methodsAndConfigsSnapshots: Seq[AgoraEntity]): MethodsAndConfigs = {
    val methodSnapshots = methodsAndConfigsSnapshots.filter(_.entityType.exists(_ == AgoraEntityType.Workflow))
    val configSnapshots = methodsAndConfigsSnapshots.filter(_.entityType.exists(_ == AgoraEntityType.Configuration))

    MethodsAndConfigs(methodSnapshots, configSnapshots)
  }

  def combine(left: MethodsAndConfigs, right: MethodsAndConfigs): MethodsAndConfigs = {
    val methodSnapshots = left.methodSnapshots ++ right.methodSnapshots
    val configSnapshots = left.configSnapshots ++ right.configSnapshots
    MethodsAndConfigs(methodSnapshots.distinct, configSnapshots.distinct)
  }
}
