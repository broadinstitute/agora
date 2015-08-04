
package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.business.{EntityAuthorizationException, AgoraBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions.{Nothing}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsAuthorizationProvider
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class GcsAuthorizationSpec extends FlatSpec {
  val agoraBusiness = new AgoraBusiness(GcsAuthorizationProvider)

  "Agora" should "give the agoraEntity's owner full permissions and reject other users" in {
    val entity = agoraBusiness.insert(testIntegrationEntity, owner1)
    val entityPermissions = GcsAuthorizationProvider.entityAuthorization(entity, owner1)
    val wrongEntityPermissions = GcsAuthorizationProvider.entityAuthorization(entity, owner2)

    assert(entityPermissions.canManage)
    assert(wrongEntityPermissions == AgoraPermissions(Nothing))
  }

  "Agora" should "check bucket write ACL when creating a new method in an existing workspace and fail if the user " +
    "doesn't have access" in {
    val thrown = intercept[EntityAuthorizationException] {
      agoraBusiness.insert(testIntegrationEntity, "jcarey@broadinstitute.org")
      agoraBusiness.insert(testIntegrationEntity, "ggrant@broadinstitute.org")
    }
    assert(thrown != null)
  }
}
