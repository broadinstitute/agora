
package org.broadinstitute.dsde.agora.server.webservice.configurations

import akka.actor.typed.ActorRef
import org.broadinstitute.dsde.agora.server.actor.AgoraGuardianActor
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.AgoraService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

/**
 * The ConfigurationsService is a light wrapper around AgoraService.
 *
 * This file defines a configurations path and Swagger annotations.
 */

class ConfigurationsService(permissionsDataSource: PermissionsDataSource,
                            agoraGuardian: ActorRef[AgoraGuardianActor.Command])
  extends AgoraService(permissionsDataSource, agoraGuardian) {
  override def path: String = ApiUtil.Configurations.path
}
