
package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.ValidationRejection
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives
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

  val routes = ApiService.handleExceptionsAndRejections {
    methodsService.postRoute ~ methodsService.querySingleRoute ~ methodsService.queryRoute
  }

  private val errorMessagePrefix = "Invalid WDL:"

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    startMockWaas()

    setMockWaasDescribeOkResponse(genericOkDescribeResponse, 8)

    testEntity1WithId = patiently(agoraBusiness.insert(testEntity1, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity2WithId = patiently(agoraBusiness.insert(testEntity2, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity3WithId = patiently(agoraBusiness.insert(testEntity3, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity4WithId = patiently(agoraBusiness.insert(testEntity4, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity5WithId = patiently(agoraBusiness.insert(testEntity5, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity6WithId = patiently(agoraBusiness.insert(testEntity6, mockAuthenticatedOwner.get, mockAccessToken))
    testEntity7WithId = patiently(agoraBusiness.insert(testEntity7, mockAuthenticatedOwner.get, mockAccessToken))
    testEntityToBeRedactedWithId = patiently(agoraBusiness.insert(testEntityToBeRedacted, mockAuthenticatedOwner.get, mockAccessToken))
  }

  override def afterAll() = {
    clearDatabases()
    stopMockWaas()
  }

  "Agora" should "return information about a method, including metadata " in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(responseAs[AgoraEntity] == testEntity1WithId)
      assert(status == OK)
    }
  }

  "Agora" should "return only payload in plain/text when parameter is set " in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + namespace1.get + "/" + name1.get + "/"
      + testEntity1WithId.snapshotId.get + "?onlyPayload=true") ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~>
      check {
        assert(status == OK)
        assert(mediaType == MediaTypes.`text/plain`)
      }
  }

  "Agora" should "return status 404, mediaType json when nothing matches query by namespace, name, snapshotId" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/foofoofoofoo/foofoofoo/99999"
    ) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status == NotFound)
    }
  }

  "Agora" should "return methods matching query by namespace and name" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?namespace=" + namespace1.get + "&name=" + name2.get) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
        val entities = responseAs[Seq[AgoraEntity]]
        assert(entities.toSet == brief(Seq(testEntity3WithId, testEntity4WithId, testEntity5WithId, testEntity6WithId, testEntity7WithId)).toSet)
        assert(status == OK)
      }
  }

  "Agora" should "return methods matching query by synopsis and documentation" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?synopsis=" + uriEncode(synopsis1.get) + "&documentation=" +
      uriEncode(documentation1.get)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
        val entities = responseAs[Seq[AgoraEntity]]
        assert(entities.toSet == brief(Seq(testEntity1WithId, testEntity2WithId, testEntity3WithId, testEntity6WithId, testEntity7WithId)).toSet)
        assert(status == OK)
      }
  }

  "Agora" should "return methods matching query by owner and payload" in {
    Get(ApiUtil.Methods.withLeadingVersion + "?owner=" + owner1.get + "&payload=" + uriEncode(payload1.get)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      val entities = responseAs[Seq[AgoraEntity]]
      assert(entities.toSet == brief(Seq(testEntity1WithId, testEntity3WithId, testEntity4WithId, testEntity5WithId)).toSet)
    }
  }

  "Agora" should "create a method and return with a status of 201" in {
    setSingleMockWaasDescribeOkResponse(payload1DescribeResponse)

    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
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

  "Agora" should "create a method and return with a status of 201 with a WDL1.0 Payload" in {
    setSingleMockWaasDescribeOkResponse(goodWdlVersionDescribeResponse)

    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity(payloadWdl10)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status == Created)
    }
  }

  "Agora" should "return a 400 bad request when posting with a bad version in the Payload" in {
    setSingleMockWaasDescribeOkResponse(badVersionDescribeResponse)

    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity(payloadWdlBadVersion)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status == BadRequest)
      assert(responseAs[String] contains "ERROR: Finished parsing without consuming all tokens")
    }
  }

  "Agora" should "return a 400 bad request when posting a malformed payload" in {
    setSingleMockWaasDescribeOkResponse(malformedPayloadDescribeResponse)

    Post(ApiUtil.Methods.withLeadingVersion, testBadAgoraEntity) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status === BadRequest)
      assert(responseAs[String] contains s"$errorMessagePrefix ERROR: No more tokens.  Expecting")
    }
  }

  "Agora" should "reject the request with validation errors when metadata is invalid" in {
    val entity = new AgoraEntity(namespace= Option(" "), name= Option(" ") , synopsis= Option(" "), payload= Option(" "), entityType= Option(AgoraEntityType.Task))

    Post(ApiUtil.Methods.withLeadingVersion, entity) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.nonEmpty)
    }
  }

  "Agora" should "reject the request when posting with a payload of None" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(payload = None)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("You must supply a payload.",None)))
    }
  }

  "Agora" should "reject the request when posting with a payload of whitespace only" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(payload = Some(" "))) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("You must supply a payload.",None)))
    }
  }

  "Agora" should "reject the request when posting with a snapshotId" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(snapshotId = Some(123))) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("You cannot specify a snapshotId. It will be assigned by the system.",None)))
    }
  }

  "Agora" should "reject the request when posting with a namespace of None" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(namespace = None)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Namespace cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a namespace of whitespace only" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(namespace = Some(" "))) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Namespace cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a name of None" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(name = None)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Name cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a name of whitespace only" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(name = Some(" "))) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Name cannot be empty",None)))
    }
  }

  "Agora" should "reject the request when posting with a synopsis of 81 characters" in {
    val testSynopsis = Some(fillerText.take(81))
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(synopsis = testSynopsis)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(!handled)
      assert(rejections.contains(ValidationRejection("Synopsis must be less than 80 chars",None)))
    }
  }

  Seq(80, 79, 74) foreach { n =>
    "Agora" should s"return a Created success code when posting with a synopsis of $n characters" in {
      setSingleMockWaasDescribeOkResponse(payload1DescribeResponse)

      val testSynopsis = Some(fillerText.take(80))
      Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(synopsis = testSynopsis)) ~>
        addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
        assert(responseAs[AgoraEntity].synopsis == testSynopsis)
        assert(status == Created)
      }
    }
  }

  "Agora" should "reject the request when posting with a documentation of 10001 chars" in {
    val testDocumentation = Some("x" * 10001)
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraEntity.copy(documentation = testDocumentation)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
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
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status == BadRequest)
    }
  }

  "Agora" should "not allow you to post a new configuration to the methods route" in {
    Post(ApiUtil.Methods.withLeadingVersion, testAgoraConfigurationEntity) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      rejection.isInstanceOf[ValidationRejection]
    }
  }

  "Agora" should "allow method redaction" in {
    Delete(ApiUtil.Methods.withLeadingVersion + "/" + testEntityToBeRedactedWithId.namespace.get + "/" +
      testEntityToBeRedactedWithId.name.get + "/" + testEntityToBeRedactedWithId.snapshotId.get) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(responseAs[String] == "1")
    }
  }

  "Agora" should "not allow redacted methods to be queried" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + testEntityToBeRedactedWithId.namespace.get + "/" +
      testEntityToBeRedactedWithId.name.get + "/" + testEntityToBeRedactedWithId.snapshotId.get) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(responseAs[String] contains "not found")
    }
  }

  "Agora" should "not let you specify a deserialized payload on the methods route" in {
    Get(ApiUtil.Methods.withLeadingVersion + "/" + testMethodWithSnapshot1.namespace.get + "/" +
      testMethodWithSnapshot1.name.get + "/" + testMethodWithSnapshot1.snapshotId.get + "?payloadAsObject=true") ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
        assert(responseAs[String] contains "does not support payload deserialization")
        assert(status == InternalServerError)
    }
  }

  "Agora" should "accept and record a snapshot comment when creating the initial snapshot of a method" in {
    setSingleMockWaasDescribeOkResponse(payload1DescribeResponse)

    Post(ApiUtil.Methods.withLeadingVersion, testMethodWithSnapshotComment1.copy(snapshotId = None)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status == Created)
      assert(responseAs[AgoraEntity].snapshotComment == snapshotComment1)
      assert(responseAs[AgoraEntity].snapshotId.contains(1))
    }
  }

  "Agora" should "apply a new snapshot comment when creating a new method snapshot" in {
    setSingleMockWaasDescribeOkResponse(payload1DescribeResponse)

    Post(ApiUtil.Methods.withLeadingVersion, testMethodWithSnapshotComment1.copy(snapshotId = None, snapshotComment = snapshotComment2)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status == Created)
      assert(responseAs[AgoraEntity].snapshotComment == snapshotComment2)
      assert(responseAs[AgoraEntity].snapshotId.contains(2))
    }
  }

  "Agora" should "record no snapshot comment for a new method snapshot if none is provided" in {
    setSingleMockWaasDescribeOkResponse(payload1DescribeResponse)

    Post(ApiUtil.Methods.withLeadingVersion, testMethodWithSnapshotComment1.copy(snapshotId = None, snapshotComment = None)) ~>
      addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> routes ~> check {
      assert(status == Created)
      assert(responseAs[AgoraEntity].snapshotComment.isEmpty)
      assert(responseAs[AgoraEntity].snapshotId.contains(3))
    }
  }
}
