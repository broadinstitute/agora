
package org.broadinstitute.dsde.agora.server.dataaccess.gcs

import org.broadinstitute.dsde.agora.server.business.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.business.{AuthorizationProvider, AgoraPermissions, AuthorizedAgoraEntity}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

object GcsAuthorizationProvider extends AuthorizationProvider {
  override def authorizationsForEntity(agoraEntity: Option[AgoraEntity], username: String): AuthorizedAgoraEntity = {
    //TODO call google here and do GACL -> AgAcl translation
    AuthorizedAgoraEntity(agoraEntity, AgoraPermissions(All))
  }

  def authorizationsForEntities(agoraEntities: Seq[AgoraEntity], username: String): Seq[AuthorizedAgoraEntity] = {
    //TODO call google here and do GACL -> AgAcl translation
    agoraEntities.map { entity => AuthorizedAgoraEntity(Some(entity), AgoraPermissions(All)) }
  }
}

