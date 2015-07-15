
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.ValidationRejection

@DoNotDiscover
class AgoraConfigurationsSpec extends ApiServiceSpec {
  "Agora" should "be able to store a task configuration" in {
    Post(ApiUtil.Configurations.withLeadingSlash, testAgoraConfigurationEntity2) ~>
      configurationsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === namespace2)
        assert(entity.name === name1)
        assert(entity.synopsis === synopsis3)
        assert(entity.documentation === documentation1)
        assert(entity.owner === owner1)
        assert(entity.payload === taskConfigPayload)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
    }
  }

  "Agora" should "not allow you to post a new task to the configurations route" in {
    Post(ApiUtil.Configurations.withLeadingSlash, testEntityTaskWc) ~>
    configurationsService.postRoute ~> check {
      rejection === ValidationRejection
    }
  }
}
