package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

class AgoraImportSpec extends ApiServiceSpec {
  var testEntityTaskWcWithId: AgoraEntity = _

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    testEntityTaskWcWithId = agoraBusiness.insert(testEntityTaskWc, mockAuthenticatedOwner.get)
    agoraBusiness.insert(testEntityWorkflowWithExistentWdlImport, mockAuthenticatedOwner.get)
  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  "MethodsService" should "return a 201 created when posting a WDL with an invalid import statement" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityInvalidWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status === Created)
    }
  }

  "MethodsService" should "return a 201 created when posting a WDL with an import statement that references a non-existent method" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntityNonExistentWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status === Created)
    }
  }

  "MethodsService" should "create a method and return with a status of 201 when the WDL contains an import to an existent method" in {
    // Verifying that the pre-loaded task exists...
    Get(ApiUtil.Methods.withLeadingVersion + "/" + testEntityTaskWcWithId.namespace.get + "/" + testEntityTaskWcWithId.name.get + "/"
      + testEntityTaskWcWithId.snapshotId.get) ~> methodsService.querySingleRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => assert(brief(entity) === brief(testEntityTaskWcWithId)))
      assert(status === OK)
    }
    Post(ApiUtil.Methods.withLeadingVersion, testEntityWorkflowWithExistentWdlImport) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === namespace1)
        assert(entity.name === name1)
        assert(entity.synopsis === synopsis1)
        assert(entity.documentation === documentation1)
        assert(entity.owner === owner1)
        assert(entity.payload === payloadReferencingExternalMethod)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
      assert(status === Created)
    }
  }

}
