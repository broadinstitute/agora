package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, DoNotDiscover}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.testkit.{ScalatestRouteTest, RouteTest}
import scala.concurrent.duration._

@DoNotDiscover
class AgoraImportIntegrationSpec extends FlatSpec with RouteTest with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val agoraBusiness = new AgoraBusiness()
  val methodsService = new MethodsService() with ActorRefFactoryContext

  override def beforeAll() = {
    ensureDatabasesAreRunning()
  }

  override def afterAll() = {
    clearDatabases()
  }

  def handleError[T](deserialized: Deserialized[T], assertions: (T) => Unit) = {
    if (status.isSuccess) {
      if (deserialized.isRight) assertions(deserialized.right.get) else failTest(deserialized.left.get.toString)
    } else {
      failTest(response.message.toString)
    }
  }

  "MethodsService" should "return a 201 when posting a WDL with a valid (extant) official docker image" in {
    Post(ApiUtil.Methods.withLeadingVersion, testTaskWithValidOfficialDockerImageInWdl) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === testTaskWithValidOfficialDockerImageInWdl.namespace)
        assert(entity.name === testTaskWithValidOfficialDockerImageInWdl.name)
        assert(entity.synopsis === testTaskWithValidOfficialDockerImageInWdl.synopsis)
        assert(entity.documentation === testTaskWithValidOfficialDockerImageInWdl.documentation)
        assert(entity.owner === testTaskWithValidOfficialDockerImageInWdl.owner)
        assert(entity.payload === testTaskWithValidOfficialDockerImageInWdl.payload)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
      assert(status === Created)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid official docker image (invalid/non-existent repo name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testTaskWithInvalidOfficialDockerRepoNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid official docker image (invalid/non-existent tag name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testTaskWithInvalidOfficialDockerTagNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 201 when posting a WDL with a valid (extant) personal docker image" in {
    Post(ApiUtil.Methods.withLeadingVersion, testTaskWithValidPersonalDockerInWdl) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === testTaskWithValidPersonalDockerInWdl.namespace)
        assert(entity.name === testTaskWithValidPersonalDockerInWdl.name)
        assert(entity.synopsis === testTaskWithValidPersonalDockerInWdl.synopsis)
        assert(entity.documentation === testTaskWithValidPersonalDockerInWdl.documentation)
        assert(entity.owner === testTaskWithValidPersonalDockerInWdl.owner)
        assert(entity.payload === testTaskWithValidPersonalDockerInWdl.payload)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
      assert(status === Created)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent user name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testTaskWithInvalidPersonalDockerUserNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent repo name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testTaskWithInvalidPersonalDockerRepoNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent tag name)" in {
    Post(ApiUtil.Methods.withLeadingVersion, testTaskWithInvalidPersonalDockerTagNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }
}
