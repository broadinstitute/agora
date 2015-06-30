
package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

case class AuthorizedAgoraEntity(entity: Option[AgoraEntity], authorization: AgoraPermissions)

case class AuthorizedNamespace(namepace: Option[String], authorization: AgoraPermissions)
