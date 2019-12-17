package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.OwnersAndAliases
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType.EntityType
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType, MethodsAndConfigs}

/**
 * Contains data that is cached periodically and returned via asks to the guardian actor.
 */
case class AgoraDataCache(configurationWorkflowEntities: Seq[AgoraEntity],
                          taskWorkflowEntities: Seq[AgoraEntity],
                          configurationOnlyEntities: Seq[AgoraEntity],
                          publicMethodsAndConfigs: MethodsAndConfigs,
                          ownersAndAliases: OwnersAndAliases)

object AgoraDataCache {
  val TaskWorkflowEntityTypes: Seq[EntityType] = Seq(AgoraEntityType.Task, AgoraEntityType.Workflow)
  val ConfigurationOnlyEntityTypes: Seq[EntityType] = Seq(AgoraEntityType.Configuration)
  val ConfigurationWorkflowEntityTypes: Seq[EntityType] = Seq(AgoraEntityType.Configuration, AgoraEntityType.Workflow)
  val ConfigurationWorkflowEntitiesProjection: AgoraEntityProjection = AgoraEntityProjection(
    includedFields = AgoraEntityProjection.RequiredProjectionFields ++ Seq("synopsis", "methodId"),
    excludedFields = Nil)

}
