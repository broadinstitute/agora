
package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class AuthorizedAgoraEntity(entity: AgoraEntity, authorization: AgoraPermissions)
