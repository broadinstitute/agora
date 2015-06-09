
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

@DoNotDiscover
class AgoraReferencesSpec extends ApiServiceSpec {
  "Agora" should "return a 400 bad request when posting a WDL with an invalid import statement" in {
    Post(ApiUtil.Methods.withLeadingSlash, testBadAgoraEntityInvalidWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "Agora" should "return a 400 bad request when posting a WDL with an import statement that references a non-existent method" in {
    Post(ApiUtil.Methods.withLeadingSlash, testBadAgoraEntityNonExistentWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  // Temporarily disabling this test until such time as WDL supports referential validation via import.
  ignore should "create a method and return with a status of 201 when the WDL contains an import to an existent method" in {
    // Verifying that the pre-loaded task exists...
    Get(ApiUtil.Methods.withLeadingSlash + "/" + testEntityTaskWcWithId.namespace.get + "/" + testEntityTaskWcWithId.name.get + "/"
      + testEntityTaskWcWithId.snapshotId.get) ~> methodsService.queryByNamespaceNameSnapshotIdRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => assert(entity === testEntityTaskWcWithId))
      assert(status === OK)
    }
    Post(ApiUtil.Methods.withLeadingSlash, testEntityWorkflowWithExistentWdlImport) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === namespace1)
        assert(entity.name === name1)
        assert(entity.synopsis === synopsis1)
        assert(entity.documentation === documentation1)
        assert(entity.owner === agoraCIOwner)
        assert(entity.payload === payloadReferencingExternalMethod)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
      assert(status === Created)
    }
  }
}
