
package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class AuthorizedAgoraEntity(entity: Option[AgoraEntity], authorization: AgoraPermissions)
