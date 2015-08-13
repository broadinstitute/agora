package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraIntegrationTestData._
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, DoNotDiscover}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.testkit.{ScalatestRouteTest, RouteTest}
import scala.concurrent.duration._

@DoNotDiscover
class AgoraImportIntegrationSpec extends FlatSpec with RouteTest with ScalatestRouteTest with BeforeAndAfterAll {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory = system
  }

  val agoraBusiness = new AgoraBusiness()
  val methodsService = new MethodsService() with ActorRefFactoryContext


  def handleError[T](deserialized: Deserialized[T], assertions: (T) => Unit) = {
    if (status.isSuccess) {
      if (deserialized.isRight) assertions(deserialized.right.get) else failTest(deserialized.left.get.toString)
    } else {
      failTest(response.message.toString)
    }
  }

  "MethodsService" should "return a 201 when posting a WDL with a valid (extant) official docker image" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityWithValidOfficialDockerImageInWdl) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === testAgoraEntityWithValidOfficialDockerImageInWdl.namespace)
        assert(entity.name === testAgoraEntityWithValidOfficialDockerImageInWdl.name)
        assert(entity.synopsis === testAgoraEntityWithValidOfficialDockerImageInWdl.synopsis)
        assert(entity.documentation === testAgoraEntityWithValidOfficialDockerImageInWdl.documentation)
        assert(entity.owner === testAgoraEntityWithValidOfficialDockerImageInWdl.owner)
        assert(entity.payload === testAgoraEntityWithValidOfficialDockerImageInWdl.payload)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
      assert(status === Created)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid official docker image (invalid/non-existent repo name)" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid official docker image (invalid/non-existent tag name)" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityWithInvalidOfficialDockerTagNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 201 when posting a WDL with a valid (extant) personal docker image" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityWithValidPersonalDockerInWdl) ~>
      methodsService.postRoute ~> check {
      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === testAgoraEntityWithValidPersonalDockerInWdl.namespace)
        assert(entity.name === testAgoraEntityWithValidPersonalDockerInWdl.name)
        assert(entity.synopsis === testAgoraEntityWithValidPersonalDockerInWdl.synopsis)
        assert(entity.documentation === testAgoraEntityWithValidPersonalDockerInWdl.documentation)
        assert(entity.owner === testAgoraEntityWithValidPersonalDockerInWdl.owner)
        assert(entity.payload === testAgoraEntityWithValidPersonalDockerInWdl.payload)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
      })
      assert(status === Created)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent user name)" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityWithInvalidPersonalDockerUserNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent repo name)" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "MethodsService" should "return a 400 bad request when posting a WDL with an invalid personal docker image (invalid/non-existent tag name)" in {
    Post(ApiUtil.Methods.withLeadingSlash, testAgoraEntityWithInvalidPersonalDockerTagNameInWdl) ~>
      methodsService.postRoute ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }




}
