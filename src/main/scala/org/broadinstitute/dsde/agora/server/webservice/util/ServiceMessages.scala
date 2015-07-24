package org.broadinstitute.dsde.agora.server.webservice.util

import com.google.api.services.storage.model.{BucketAccessControl, ObjectAccessControl}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import spray.routing.RequestContext

/**
 * Case classes representing messages to pass to service handler actors
 */
object ServiceMessages {

  // Namespace Acl messages
  case class ListNamespaceAcls(requestContext: RequestContext,
                               entity: AgoraEntity,
                               username: String)

  case class InsertNamespaceAcl(requestContext: RequestContext,
                                entity: AgoraEntity,
                                username: String,
                                acl: BucketAccessControl)

  case class DeleteNamespaceAcl(requestContext: RequestContext,
                                entity: AgoraEntity,
                                username: String,
                                acl: BucketAccessControl)

  // Entity Acl messages
  case class ListEntityAcls(requestContext: RequestContext,
                             entity: AgoraEntity,
                             username: String)

  case class InsertEntityAcl(requestContext: RequestContext,
                              entity: AgoraEntity,
                              username: String,
                              acl: ObjectAccessControl)

  case class DeleteEntityAcl(requestContext: RequestContext,
                              entity: AgoraEntity,
                              username: String,
                              acl: ObjectAccessControl)

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
