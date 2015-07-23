
package org.broadinstitute.dsde.agora.server.dataaccess.acls

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsAuthorizationProvider
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class GcsAuthorizationSpec extends FlatSpec {
  val agoraBusiness = new AgoraBusiness(GcsAuthorizationProvider)
  val username: String = AgoraConfig.gcsServiceAccountUserEmail

  "Agora" should "verify GCS authorization when creating a new method" in {
    val entity = agoraBusiness.insert(testIntegrationEntity, "jcarey@broadinstitute.org")
    assert(agoraBusiness.findSingle(entity, Seq(entity.entityType.get), username) === entity)
  }
}
