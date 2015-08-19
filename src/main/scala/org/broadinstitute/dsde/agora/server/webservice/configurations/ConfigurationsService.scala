
package org.broadinstitute.dsde.agora.server.webservice.configurations

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.AgoraService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

/**
 * The ConfigurationsService is a light wrapper around AgoraService.
 *
 * This file defines a configurations path and Swagger annotations.
 */

abstract class ConfigurationsService extends AgoraService {
  override def path = ApiUtil.Configurations.path

  override def querySingleRoute = super.querySingleRoute

  override def queryRoute = super.queryRoute

  override def postRoute = super.postRoute
}
