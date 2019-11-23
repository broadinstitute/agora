
package org.broadinstitute.dsde.agora.server.webservice.configurations

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.AgoraService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

/**
 * The ConfigurationsService is a light wrapper around AgoraService.
 *
 * This file defines a configurations path and Swagger annotations.
 */

class ConfigurationsService(permissionsDataSource: PermissionsDataSource) extends AgoraService(permissionsDataSource) {
  override def path: String = ApiUtil.Configurations.path
}
