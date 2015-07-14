
package org.broadinstitute.dsde.agora.server.webservice

import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.dataaccess.authorization.TestAuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

import scala.Predef.assert

@DoNotDiscover
class AgoraImportSpec extends ApiServiceSpec {
  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid import statement" in {
    Post(ApiUtil.Methods.withLeadingSlash, testBadAgoraEntityInvalidWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an import statement that references a non-existent method" in {
    Post(ApiUtil.Methods.withLeadingSlash, testBadAgoraEntityNonExistentWdlImportFormat) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "create a method and return with a status of 201 when the WDL contains an import to an existent method" in {
    // Verifying that the pre-loaded task exists...
    Get(ApiUtil.Methods.withLeadingSlash + "/" + testEntityTaskWcWithId.namespace.get + "/" + testEntityTaskWcWithId.name.get + "/"
      + testEntityTaskWcWithId.snapshotId.get) ~> methodsService.querySingleRoute ~> check {
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

  "ImportResolverHelper" should "reject import if scheme != methods://" in {
    val thrown = the[SyntaxError] thrownBy ImportResolverHelper.validateUri("blah://")
    assert(thrown.getMessage.contains("start with") === true)
  }

  "ImportResolverHelper" should "reject import if something comes before scheme" in {
    val thrown = the[SyntaxError] thrownBy ImportResolverHelper.validateUri("foo methods://")
    assert(thrown.getMessage.contains("start with") === true)
  }

  "ImportResolverHelper" should "reject import if uri doesn't have 3 parts" in {
    val thrown = the[SyntaxError] thrownBy ImportResolverHelper.validateUri("methods://")
    assert(thrown.getMessage.contains("three parts") === true)
  }

  "ImportResolverHelper" should "reject import if third part is not an integer" in {
    val thrown = the[SyntaxError] thrownBy ImportResolverHelper.validateUri("methods://foo.bar.baz")
    assert(thrown.getMessage.contains("integer") === true)
  }

  "ImportResolverHelper" should "accept a valid import uri" in {
    noException should be thrownBy ImportResolverHelper.validateUri("methods://foo.bar.2")
  }

  "ImportResolverHelper.resolve" should "return None if method does not exist" in {
    val method = ImportResolverHelper.resolve("methods://foo.bar.1", agoraBusiness, agoraCIOwner.get)
    assert(method === None)
  }

  "ImportResolverHelper.resolve" should "return the method if it exists" in {
    val namespace = testEntityTaskWcWithId.namespace.get
    val name = testEntityTaskWcWithId.name.get
    val id = testEntityTaskWcWithId.snapshotId.get
    val method = ImportResolverHelper.resolve(s"methods://$namespace.$name.$id", agoraBusiness, agoraCIOwner.get)
    assert(method !== None)
    assert(method.get.namespace.get === namespace)
    assert(method.get.name.get === name)
    assert(method.get.snapshotId.get === id)
  }

  "MethodImportResolver" should "reject import if scheme != methods://" in {
    val uri = "blah://"
    val resolver = MethodImportResolver(agoraCIOwner.get, agoraBusiness, TestAuthorizationProvider)
    val thrown = the[SyntaxError] thrownBy resolver.importResolver(uri)
    assert(thrown.getMessage.contains("start with") === true)
  }

  "MethodImportResolver" should "reject import if method not found" in {
    val uri = "methods://foo.bar.22"
    val resolver = MethodImportResolver(agoraCIOwner.get, agoraBusiness, TestAuthorizationProvider)
    val thrown = the[SyntaxError] thrownBy resolver.importResolver(uri)
    assert(thrown.getMessage.contains("Can't resolve import") === true)   
  }

  "MethodImportResolver" should "return payload if method is found" in {
    val namespace = testEntityTaskWcWithId.namespace.get
    val name = testEntityTaskWcWithId.name.get
    val id = testEntityTaskWcWithId.snapshotId.get
    val uri = s"methods://$namespace.$name.$id"
    val resolver = MethodImportResolver(agoraCIOwner.get, agoraBusiness, TestAuthorizationProvider)
    val payload = resolver.importResolver(uri)
    assert(payload.contains("wc") === true)
  }

}