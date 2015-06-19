
package org.broadinstitute.dsde.agora.server.dataaccess.authorization

import org.broadinstitute.dsde.agora.server.business.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.business.{AuthorizationProvider, AgoraPermissions, AuthorizedAgoraEntity}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

import scala.collection.mutable

//Test authorization implementation. Add authorizations directly to the hash map for lookup. If the entity is not found it is assumed you
//have full permissions on that entity.
object TestAuthorizationProvider extends AuthorizationProvider {
  val localPermissions: mutable.Map[String, AgoraPermissions] = new mutable.HashMap[String, AgoraPermissions]()

  def addLocalPermissions(entityId: String, authorization: AgoraPermissions) = localPermissions.put(entityId, authorization)

  override def authorizationsForEntity(agoraEntity: Option[AgoraEntity], username: String): AuthorizedAgoraEntity = {
    agoraEntity match {
      case Some(agoraEntity) => AuthorizedAgoraEntity(Some(agoraEntity), localPermissions.getOrElse(getUniqueIdentifier(agoraEntity), AgoraPermissions(All)))
      case None => AuthorizedAgoraEntity(None, AgoraPermissions(Nothing))
    }
    
  }

  override def authorizationsForEntities(agoraEntities: Seq[AgoraEntity], username: String): Seq[AuthorizedAgoraEntity] = {
    agoraEntities.map { entity => AuthorizedAgoraEntity(Some(entity), localPermissions.getOrElse(getUniqueIdentifier(entity), AgoraPermissions(All))) }
  }

  def getUniqueIdentifier(entity: AgoraEntity): String = entity.namespace + ":" + entity.name + ":" + entity.snapshotId
}
