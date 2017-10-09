package org.broadinstitute.dsde.agora.server.webservice.util

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, EntityAccessControl}
import org.broadinstitute.dsde.agora.server.ga4gh.Models.ToolDescriptorType.DescriptorType
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import spray.routing.RequestContext

/**
 * Case classes representing messages to pass to service handler actors
 */
object ServiceMessages {

  // Namespace Permission messages
  case class ListNamespacePermissions(requestContext: RequestContext,
                                       entity: AgoraEntity,
                                       username: String)

  case class InsertNamespacePermission(requestContext: RequestContext,
                                        entity: AgoraEntity,
                                        username: String,
                                        accessObject: AccessControl)

  case class BatchNamespacePermission(requestContext: RequestContext,
                                             entity: AgoraEntity,
                                             username: String,
                                             accessObjects: List[AccessControl])

  case class EditNamespacePermission(requestContext: RequestContext,
                                       entity: AgoraEntity,
                                       username: String,
                                       accessObject: AccessControl)

  case class DeleteNamespacePermission(requestContext: RequestContext,
                                        entity: AgoraEntity,
                                        username: String,
                                        userToRemove: String)

  // Entity Permission messages
  case class ListEntityPermissions(requestContext: RequestContext,
                                   entity: AgoraEntity,
                                   username: String)

  case class ListMultiEntityPermissions(requestContext: RequestContext,
                                   entities: List[AgoraEntity],
                                   username: String)

  case class UpsertMultiEntityPermissions(requestContext: RequestContext,
                                          aclPairs: List[EntityAccessControl],
                                          username: String)

  case class InsertEntityPermission(requestContext: RequestContext,
                                    entity: AgoraEntity,
                                    username: String,
                                    accessObject: AccessControl)

  case class BatchEntityPermission(requestContext: RequestContext,
                                         entity: AgoraEntity,
                                         username: String,
                                         accessObjects: List[AccessControl])

  case class EditEntityPermission(requestContext: RequestContext,
                                    entity: AgoraEntity,
                                    username: String,
                                    accessObject: AccessControl)

  case class DeleteEntityPermission(requestContext: RequestContext,
                                    entity: AgoraEntity,
                                    username: String,
                                    userToRemove: String)

  // Agora Entity messages
  case class QuerySingle(requestContext: RequestContext,
                         entity: AgoraEntity,
                         entityType: Seq[AgoraEntityType.EntityType],
                         username: String,
                         onlyPayload: Boolean,
                         payloadAsObject: Boolean)

  case class QueryPublicSingle(requestContext: RequestContext,
                               entity: AgoraEntity)

  case class QueryPublicSinglePayload(requestContext: RequestContext,
                                      entity: AgoraEntity,
                                      descriptorType: DescriptorType)

  case class Delete(requestContext: RequestContext,
                         entity: AgoraEntity,
                         entityTypes: Seq[AgoraEntityType.EntityType],
                         username: String)

  case class Copy(requestContext: RequestContext,
                    oldEntity: AgoraEntity,
                    newEntity: AgoraEntity,
                    redact: Boolean,
                    entityTypes: Seq[AgoraEntityType.EntityType],
                    username: String)

  case class Query(requestContext: RequestContext,
                   agoraSearch: AgoraEntity,
                   agoraProjection: Option[AgoraEntityProjection],
                   entityTypes: Seq[AgoraEntityType.EntityType],
                   username: String)

  case class QueryPublic(requestContext: RequestContext,
                   agoraSearch: AgoraEntity)

  case class QueryDefinitions(requestContext: RequestContext,
                   username: String)

  case class QueryAssociatedConfigurations(requestContext: RequestContext,
                                           namespace: String,
                                           name: String,
                                           username: String)

  case class QueryCompatibleConfigurations(requestContext: RequestContext,
                                           namespace: String,
                                           name: String,
                                           snapshotId: Int,
                                           username: String)

  case class Add(requestContext: RequestContext,
                 agoraAddRequest: AgoraEntity,
                 username: String)

  case class Status(requestContext: RequestContext)

}
