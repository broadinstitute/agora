
package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraAuthorizationException, AgoraBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsAuthorizationProvider
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class GcsAuthorizationSpec extends FlatSpec {
  val agoraBusiness = new AgoraBusiness(GcsAuthorizationProvider)
  val user = "jcarey@broadinstitute.org"

  "Agora" should "verify GCS authorization when creating a new method" in {
    val entity = agoraBusiness.insert(testIntegrationEntity, user)
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), user) === entity)
  }

  "Agora" should "check bucket write ACL when creating a new method in an existing workspace and fail if the user " +
    "doesn't have access" in {
    val thrown = intercept[AgoraAuthorizationException] {
      agoraBusiness.insert(testIntegrationEntity, "jcarey@broadinstitute.org")
      agoraBusiness.insert(testIntegrationEntity, "ggrant@broadinstitute.org")
    }
    assert(thrown != null)
  }
}
