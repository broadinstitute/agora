package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.{RouteTest, RouteTestTimeout}
import org.broadinstitute.dsde.agora.server.{AgoraRouteTest, AgoraTestFixture}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FlatSpec}

import scala.concurrent.duration._

@DoNotDiscover
class AgoraImportIntegrationSpec extends FlatSpec with RouteTest with AgoraRouteTest with BeforeAndAfterAll
  with AgoraTestFixture {

  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(20.seconds)

  lazy val methodsService = new MethodsService(permsDataSource, agoraGuardian)

  override def beforeAll(): Unit = {
    super.beforeAll()
    ensureDatabasesAreRunning()
    startMockWaas()
  }

  override def afterAll(): Unit = {
    clearDatabases()
    stopMockWaas()
    super.afterAll()
  }

  "MethodsService" should "return a 201 when posting a WDL with a valid (extant) official docker image" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntityWithValidOfficialDockerImageInWdl) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> methodsService.postRoute ~> check {
        val entity = responseAs[AgoraEntity]
        assert(entity.namespace == testAgoraEntityWithValidOfficialDockerImageInWdl.namespace)
        assert(entity.name == testAgoraEntityWithValidOfficialDockerImageInWdl.name)
        assert(entity.synopsis == testAgoraEntityWithValidOfficialDockerImageInWdl.synopsis)
        assert(entity.documentation == testAgoraEntityWithValidOfficialDockerImageInWdl.documentation)
        assert(entity.owner == testAgoraEntityWithValidOfficialDockerImageInWdl.owner)
        assert(entity.payload == testAgoraEntityWithValidOfficialDockerImageInWdl.payload)
        assert(entity.snapshotId.isDefined)
        assert(entity.createDate.isDefined)

        assert(status == Created)
      }
  }

  ignore should "return a 400 bad request when posting a WDL with an invalid official docker image (invalid/non-existent repo name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
      assert(Option(responseAs[String]).nonEmpty)
    }
  }

  ignore should "return a 400 bad request when posting a WDL with an invalid official docker image (invalid/non-existent tag name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntityWithInvalidOfficialDockerTagNameInWdl) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
      assert(Option(responseAs[String]).nonEmpty)
    }
  }

  "MethodsService" should "return a 201 when posting a WDL with a valid (extant) personal docker image" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntityWithValidPersonalDockerInWdl) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~>
      methodsService.postRoute ~> check {
        val entity = responseAs[AgoraEntity]
        assert(entity.namespace == testAgoraEntityWithValidPersonalDockerInWdl.namespace)
        assert(entity.name == testAgoraEntityWithValidPersonalDockerInWdl.name)
        assert(entity.synopsis == testAgoraEntityWithValidPersonalDockerInWdl.synopsis)
        assert(entity.documentation == testAgoraEntityWithValidPersonalDockerInWdl.documentation)
        assert(entity.owner == testAgoraEntityWithValidPersonalDockerInWdl.owner)
        assert(entity.payload == testAgoraEntityWithValidPersonalDockerInWdl.payload)
        assert(entity.snapshotId.isDefined)
        assert(entity.createDate.isDefined)

        assert(status == Created)
    }
  }

  ignore should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent user name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntityWithInvalidPersonalDockerUserNameInWdl) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
      assert(Option(responseAs[String]).nonEmpty)
    }
  }

  ignore should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent repo name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
      assert(Option(responseAs[String]).nonEmpty)
    }
  }

  ignore should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent tag name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntityWithInvalidPersonalDockerTagNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status == BadRequest)
      assert(Option(responseAs[String]).nonEmpty)
    }
  }
}
