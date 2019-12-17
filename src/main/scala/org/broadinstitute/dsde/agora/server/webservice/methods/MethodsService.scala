package org.broadinstitute.dsde.agora.server.webservice.methods

import akka.actor.typed.ActorRef
import org.broadinstitute.dsde.agora.server.actor.AgoraGuardianActor
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.AgoraService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

/**
 * The MethodsService is a light wrapper around AgoraService.
 *
 * This file defines a methods path (config) and Swagger annotations.
 */

class MethodsService(permissionsDataSource: PermissionsDataSource,
                     agoraGuardian: ActorRef[AgoraGuardianActor.Command])
  extends AgoraService(permissionsDataSource, agoraGuardian) {
  override def path: String = ApiUtil.Methods.path
}
