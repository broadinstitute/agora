package org.broadinstitute.dsde.agora.server.webservice.methods

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.AgoraService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

/**
 * The MethodsService is a light wrapper around AgoraService.
 *
 * This file defines a methods path (config) and Swagger annotations.
 */

class MethodsService(permissionsDataSource: PermissionsDataSource) extends AgoraService(permissionsDataSource) {
  override def path: String = ApiUtil.Methods.path
}
