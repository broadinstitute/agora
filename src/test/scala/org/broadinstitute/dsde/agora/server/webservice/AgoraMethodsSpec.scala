
package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.ValidationRejection
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{DoNotDiscover, FlatSpecLike}

@DoNotDiscover
class AgoraMethodsSpec extends ApiServiceSpec with FlatSpecLike {

  var testEntity1WithId: AgoraEntity = _
  var testEntity2WithId: AgoraEntity = _
  var testEntity3WithId: AgoraEntity = _
  var testEntity4WithId: AgoraEntity = _
  var testEntity5WithId: AgoraEntity = _
  var testEntity6WithId: AgoraEntity = _
  var testEntity7WithId: AgoraEntity = _
  var testEntityToBeRedactedWithId: AgoraEntity = _

  val routes = handleExceptions(ApiService.exceptionHandler) {
    methodsService.postRoute
  }

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    testEntity1WithId = patiently(agoraBusiness.insert(testEntity1, mockAuthenticatedOwner.get))
    testEntity2WithId = patiently(agoraBusiness.insert(testEntity2, mockAuthenticatedOwner.get))
    testEntity3WithId = patiently(agoraBusiness.insert(testEntity3, mockAuthenticatedOwner.get))
    testEntity4WithId = patiently(agoraBusiness.insert(testEntity4, mockAuthenticatedOwner.get))
    testEntity5WithId = patiently(agoraBusiness.insert(testEntity5, mockAuthenticatedOwner.get))
    testEntity6WithId = patiently(agoraBusiness.insert(testEntity6, mockAuthenticatedOwner.get))
    testEntity7WithId = patiently(agoraBusiness.insert(testEntity7, mockAuthenticatedOwner.get))
    testEntityToBeRedactedWithId = patiently(agoraBusiness.insert(testEntityToBeRedacted, mockAuthenticatedOwner.get))
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "return information about a method, including metadata " in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get) ~> methodsService.querySingleRoute ~> check {
      assert(responseAs[AgoraEntity] == testEntity1WithId)
      assert(status == OK)
    }
  }

  "Agora" should "return only payload in plain/text when parameter is set " in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get + "?onlyPayload=true") ~> methodsService.querySingleRoute ~>
      check {
        assert(status == OK)
        assert(mediaType == MediaTypes.`text/plain`)
      }
  }

  "Agora" should "return status 404, mediaType json when nothing matches query by namespace, name, snapshotId" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/foofoofoofoo/foofoofoo/99999"
    ) ~> methodsService.querySingleRoute ~> check {
      assert(status == NotFound)
    }
  }

  "Agora" should "return methods matching query by namespace and name" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?namespace=" + namespace1.get + "&name=" + name2.get) ~>
      methodsService.queryRoute ~> check {
        val entities = responseAs[Seq[AgoraEntity]]
        assert(entities.toSet == brief(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)).toSet)
        assert(status == OK)
      }
  }

  "Agora" should "return methods matching query by synopsis and documentation" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?synopsis=" + uriEncode(synopsis1.get) + "&documentation=" +
      uriEncode(documentation1.get)) ~>
      methodsService.queryRoute ~> check {
        val entities = responseAs[Seq[AgoraEntity]]
        assert(entities.toSet == brief(Seq(testEntity1WithId, testEntity2WithId, testEntity3WithId, testEntity6WithId, testEntity7WithId)).toSet)
        assert(status == OK)
      }
  }

  "Agora" should "return methods matching query by owner and payload" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?owner=" + owner1.get + "&payload=" + uriEncode(payload1.get)) ~>
      methodsService.queryRoute ~> check {
      val entities = responseAs[Seq[AgoraEntity]]
      assert(entities.toSet == brief(Seq(testEntity1WithId, testEntity3WithId, testEntity4WithId, testEntity5WithId)).toSet)
    }
  }

  "Agora" should "create a method and return with a status of 201" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity) ~>
      routes ~> check {
        val entity = responseAs[AgoraEntity]
        assert(entity.namespace == namespace3)
        assert(entity.name == name1)
        assert(entity.synopsis == synopsis1)
        assert(entity.documentation == documentation1)
        assert(entity.owner == owner1)
        assert(entity.payload == payload1)
        assert(entity.snapshotId.isDefined)
        assert(entity.createDate.isDefined)

        assert(status == Created)
      }
  }

  "Agora" should "return a 400 bad request when posting a malformed payload" in {
    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntity) ~>
      routes ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] != null)
    }
  }

  "Agora" should "reject the request with validation errors when metadata is invalid" in {
    val entity = new AgoraEntity(namespace= Option(" "), name= Option(" ") , synopsis= Option(" "), payload= Option(" "), entityType= Option(AgoraEntityType.Task))

    Post(ApiUtil.Methods.withLeadingVersion, entity) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.nonEmpty)
    }
  }

  "Agora" should "reject the request when posting with a payload of None" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(payload = None)) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("You must supply a payload.",None)))
    }
  }

  "Agora" should "reject the request when posting with a payload of whitespace only" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(payload = Some(" "))) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("You must supply a payload.",None)))
    }
  }

  "Agora" should "reject the request when posting with a snapshotId" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(snapshotId = Some(123))) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("You cannot specify a snapshotId. It will be assigned by the system.",None)))
    }
  }

  "Agora" should "reject the request when posting with a namespace of None" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(namespace = None)) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Namespace cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a namespace of whitespace only" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(namespace = Some(" "))) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Namespace cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a name of None" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(name = None)) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Name cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a name of whitespace only" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(name = Some(" "))) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Name cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a synopsis of 81 characters" in {
    val testSynopsis = Some(fillerText.take(81))
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(synopsis = testSynopsis)) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Synopsis must be less than 80 chars",None)))
    }
  }

  Seq(80, 79, 74) foreach { n =>
    "Agora" should s"return a Created success code when posting with a synopsis of $n characters" in {
      val testSynopsis = Some(fillerText.take(80))
      Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(synopsis = testSynopsis)) ~>
        routes ~> check {
        assert(responseAs[AgoraEntity].synopsis == testSynopsis)
        assert(status == Created)
      }
    }
  }

  "Agora" should "reject the request when posting with a documentation of 10001 chars" in {
    val testDocumentation = Some("x" * 10001)
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(documentation = testDocumentation)) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Documentation must be less than 10kb",None)))
    }
  }

  "Agora" should "reject 10kb of github markdown as method documentation" in {
    val entityJSON = s"""{
                        | "namespace": "$namespace1",
                        | "name": "$name1",
                        | "synopsis": "",
                        | "documentation": "$getBigDocumentation",
                        | "payload": "",
                        | "entityType": "Task"
                        |}""".stripMargin

    val entity = HttpEntity(
      contentType = ContentTypes.`application/json`,
      string = entityJSON)

    Post(ApiUtil.Methods.withLeadingVersion, entity) ~>
      wrapWithRejectionHandler {
        methodsService.postRoute
      } ~> check {
      assert(status == BadRequest)
    }
  }

  "Agora" should "not allow you to post a new configuration to the methods route" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraConfigurationEntity) ~>
      routes ~> check {
      rejection.isInstanceOf[ValidationRejection]
    }
  }

//  "Agora" should "allow method redaction" in {
//    Delete(ApiUtil.Methods.withLeadingVersion + "/" + testEntityToBeRedactedWithId.namespace.get + "/" +
//      testEntityToBeRedactedWithId.name.get + "/" + testEntityToBeRedactedWithId.snapshotId.get) ~>
//    methodsService.querySingleRoute ~> check {
//      assert(body.asString == "1")
//    }
//  }
//
//  "Agora" should "not allow redacted methods to be queried" in {
//    Get(ApiUtil.Methods.withLeadingVersion + "/" + testEntityToBeRedactedWithId.namespace.get + "/" +
//      testEntityToBeRedactedWithId.name.get + "/" + testEntityToBeRedactedWithId.snapshotId.get) ~>
//      methodsService.querySingleRoute ~> check {
//      assert(body.asString contains "not found")
//    }
//  }
//
//  "Agora" should "not let you specify a deserialized payload on the methods route" in {
//    Get(ApiUtil.Methods.withLeadingVersion + "/" + testMethodWithSnapshot1.namespace.get + "/" +
//      testMethodWithSnapshot1.name.get + "/" + testMethodWithSnapshot1.snapshotId.get + "?payloadAsObject=true") ~>
//      methodsService.querySingleRoute ~> check {
//        assert(body.asString contains "does not support payload deserialization")
//        assert(status == InternalServerError)
//    }
//  }

  "Agora" should "accept and record a snapshot comment when creating the initial snapshot of a method" in {
    Post(ApiUtil.Methods.withLeadingVersion, testMethodWithSnapshotComment1.copy(snapshotId = None)) ~>
      routes ~> check {
      assert(status == Created)
      assert(responseAs[AgoraEntity].snapshotComment == snapshotComment1)
      assert(responseAs[AgoraEntity].snapshotId.contains(1))
    }
  }

  "Agora" should "apply a new snapshot comment when creating a new method snapshot" in {
    Post(ApiUtil.Methods.withLeadingVersion, testMethodWithSnapshotComment1.copy(snapshotId = None, snapshotComment = snapshotComment2)) ~>
      routes ~> check {
      assert(status == Created)
      assert(responseAs[AgoraEntity].snapshotComment == snapshotComment2)
      assert(responseAs[AgoraEntity].snapshotId.contains(2))
    }
  }

  "Agora" should "record no snapshot comment for a new method snapshot if none is provided" in {
    Post(ApiUtil.Methods.withLeadingVersion, testMethodWithSnapshotComment1.copy(snapshotId = None, snapshotComment = None)) ~>
      routes ~> check {
      assert(status == Created)
      assert(responseAs[AgoraEntity].snapshotComment.isEmpty)
      assert(responseAs[AgoraEntity].snapshotId.contains(3))
    }
  }
}
