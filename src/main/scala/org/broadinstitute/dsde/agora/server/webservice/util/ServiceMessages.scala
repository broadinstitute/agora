package org.broadinstitute.dsde.agora.server.webservice.util

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AccessControl
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

  case class InsertEntityPermission(requestContext: RequestContext,
                                    entity: AgoraEntity,
                                    username: String,
                                    accessObject: AccessControl)

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
                         onlyPayload: Boolean)

  case class Query(requestContext: RequestContext,
                   agoraSearch: AgoraEntity,
                   agoraProjection: Option[AgoraEntityProjection],
                   entityTypes: Seq[AgoraEntityType.EntityType],
                   username: String)

  case class Add(requestContext: RequestContext,
                 agoraAddRequest: AgoraEntity,
                 username: String)

}
