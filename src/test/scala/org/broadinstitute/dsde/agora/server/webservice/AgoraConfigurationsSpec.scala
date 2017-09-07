
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{DoNotDiscover, FlatSpecLike}
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.rawls.model.MethodConfiguration
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.{MalformedQueryParamRejection, ValidationRejection}

import scala.concurrent.Future

@DoNotDiscover
class AgoraConfigurationsSpec extends ApiServiceSpec with FlatSpecLike {

  var method1: AgoraEntity = _
  var testEntityToBeRedacted2WithId: AgoraEntity = _
  var testAgoraConfigurationToBeRedactedWithId: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    method1 = patiently(agoraBusiness.insert(testEntity1, mockAuthenticatedOwner.get))
    testEntityToBeRedacted2WithId = patiently(agoraBusiness.insert(testEntityToBeRedacted2, mockAuthenticatedOwner.get))
    testAgoraConfigurationToBeRedactedWithId = patiently(agoraBusiness.insert(testAgoraConfigurationToBeRedacted, mockAuthenticatedOwner.get))

    patiently(agoraBusiness.insert(testEntity2, mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testAgoraConfigurationEntity, mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testAgoraConfigurationEntity2, mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testAgoraConfigurationEntity3, mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfigWithSnapshot1, mockAuthenticatedOwner.get))
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "be able to store a task configuration" in {
    Post(ApiUtil.Configurations.withLeadingVersion, testAgoraConfigurationEntity3) ~>
      configurationsService.postRoute ~> check {

      val referencedMethod = AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(namespace1.get, name1.get, snapshotId1.get)

      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace == namespace2)
        assert(entity.name == name1)
        assert(entity.synopsis == synopsis3)
        assert(entity.documentation == documentation1)
        assert(entity.owner == owner1)
        assert(entity.payload == taskConfigPayload)
        assert(entity.snapshotId.isDefined)
        assert(entity.createDate.isDefined)
        assert(referencedMethod.id.isDefined)
        assert(entity.method.isDefined)

        val foundMethod = entity.method.get
        assert(foundMethod.namespace == namespace1)
        assert(foundMethod.name == name1)
        assert(foundMethod.snapshotId == snapshotId1)
        assert(foundMethod.url.isDefined)
      })
    }
  }

  "Agora" should "populate method references when returning configurations" in {
    Get(ApiUtil.Configurations.withLeadingVersion) ~>
      configurationsService.queryRoute ~> check {

      handleError(entity.as[Seq[AgoraEntity]], (configs: Seq[AgoraEntity]) => {
        val method1 = AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(namespace1.get, name1.get, snapshotId1.get)
        val method2 = AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(namespace2.get, name1.get, snapshotId1.get)
        val method3 = AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(namespace1.get, name1.get, snapshotId1.get)

        val config1 = AgoraDao.createAgoraDao(Seq(AgoraEntityType.Configuration)).findSingle(
          testAgoraConfigurationEntity.namespace.get, testAgoraConfigurationEntity.name.get, 2)
        val config2 = AgoraDao.createAgoraDao(Seq(AgoraEntityType.Configuration)).findSingle(
          testAgoraConfigurationEntity2.namespace.get, testAgoraConfigurationEntity2.name.get, 1)
        val config3 = AgoraDao.createAgoraDao(Seq(AgoraEntityType.Configuration)).findSingle(
          testAgoraConfigurationEntity3.namespace.get, testAgoraConfigurationEntity3.name.get, 2)

        val foundConfig1 = configs.find(config => namespaceNameIdMatch(config, config1)).get
        val foundConfig2 = configs.find(config => namespaceNameIdMatch(config, config2)).get
        val foundConfig3 = configs.find(config => namespaceNameIdMatch(config, config3)).get
        
        val methodRef1 = foundConfig1.method.get
        val methodRef2 = foundConfig2.method.get
        val methodRef3 = foundConfig3.method.get

        assert(methodRef1.namespace.isDefined)
        assert(methodRef1.name.isDefined)
        assert(methodRef1.snapshotId.isDefined)
        assert(methodRef2.namespace.isDefined)
        assert(methodRef2.name.isDefined)
        assert(methodRef2.snapshotId.isDefined)
        assert(methodRef3.namespace.isDefined)
        assert(methodRef3.name.isDefined)
        assert(methodRef3.snapshotId.isDefined)

        assert(methodRef1.namespace == method1.namespace)
        assert(methodRef1.name == method1.name)
        assert(methodRef1.snapshotId == method1.snapshotId)
        assert(methodRef2.namespace == method2.namespace)
        assert(methodRef2.name == method2.name)
        assert(methodRef2.snapshotId == method2.snapshotId)
        assert(methodRef3.namespace == method3.namespace)
        assert(methodRef3.name == method3.name)
        assert(methodRef3.snapshotId == method3.snapshotId)
      })

    }
  }

  "Agora" should "not allow you to post a new configuration if you don't have permission to read the method that it references" in {
    val noPermission = new AccessControl(AgoraConfig.mockAuthenticatedUserEmail, AgoraPermissions(AgoraPermissions.Nothing))
    runInDB { db =>
      db.aePerms.editEntityPermission(method1, noPermission)
    }
    Post(ApiUtil.Configurations.withLeadingVersion, testAgoraConfigurationEntity3) ~>
      configurationsService.postRoute ~> check {
        assert(status == NotFound)
    }
  }

  private def namespaceNameIdMatch(entity1: AgoraEntity, entity2: AgoraEntity): Boolean = {
    entity1.namespace == entity2.namespace &&
    entity1.name == entity2.name &&
    entity1.snapshotId == entity2.snapshotId
  }
  
  "Agora" should "not allow you to post a new task to the configurations route" in {
    Post(ApiUtil.Configurations.withLeadingVersion, testEntityTaskWc) ~>
    configurationsService.postRoute ~> check {
      assert(rejection.isInstanceOf[ValidationRejection])
    }
  }

  "Agora" should "redact methods when it has at least 1 associated configuration" in {
    Delete(ApiUtil.Methods.withLeadingVersion + "/" +
      testEntityToBeRedacted2WithId.namespace.get + "/" +
      testEntityToBeRedacted2WithId.name.get + "/" +
      testEntityToBeRedacted2WithId.snapshotId.get) ~>
    methodsService.querySingleRoute ~>
    check {
      assert(body.asString == "1")
    }
  }

  "Agora" should "redact associated configurations when the referenced method is redacted" in {
    Get(ApiUtil.Configurations.withLeadingVersion + "/" +
      testAgoraConfigurationToBeRedactedWithId.namespace.get + "/" +
      testAgoraConfigurationToBeRedactedWithId.name.get + "/" +
      testAgoraConfigurationToBeRedactedWithId.snapshotId.get) ~>
    configurationsService.querySingleRoute ~>
    check {
      assert(body.asString contains "not found")
    }
  }

  {
    val baseURL = ApiUtil.Configurations.withLeadingVersion + "/" +
      testConfigWithSnapshot1.namespace.get + "/" +
      testConfigWithSnapshot1.name.get + "/" +
      testConfigWithSnapshot1.snapshotId.get

    "Agora" should "return the payload as an object if you ask it to" in {
      Get(baseURL + "?payloadAsObject=true") ~>
      configurationsService.querySingleRoute ~>
      check {
        assert(status == OK)

        val entity = responseAs[AgoraEntity]

        val payloadObject = entity.payloadObject.get
        assert(payloadObject.isInstanceOf[MethodConfiguration])
        assert(payloadObject.namespace == namespace1.get)
        assert(payloadObject.name == name5.get)
        assert(entity.payload.isEmpty)
      }
    }

    "Agora" should "return the payload as a string by default" in {
      Get(baseURL) ~>
      configurationsService.querySingleRoute ~>
      check {
        assert(status == OK)

        val entity = responseAs[AgoraEntity]
        assert(entity.payloadObject.isEmpty)
        assert(entity.payload.get contains testConfigWithSnapshot1.payload.get)
      }
    }

    "Agora" should "not let you use payloadAsObject and onlyPayload at the same time" in {
      Get(baseURL + "?payloadAsObject=true&onlyPayload=true") ~>
        configurationsService.querySingleRoute ~> check {
        assert(body.asString contains "onlyPayload, payloadAsObject cannot be used together")
        assert(status == BadRequest)
      }
    }

    "Agora" should "throw an error if you try to use an illegal value for both parameters" in {
      Get(baseURL + "?payloadAsObject=fire&onlyPayload=cloud") ~>
        wrapWithRejectionHandler {
          configurationsService.querySingleRoute
        } ~> check {
        rejection.isInstanceOf[MalformedQueryParamRejection]
      }
    }

    "Agora" should "throw an error if you try to use an illegal value for one parameter" in {
      Get(baseURL + "?payloadAsObject=fire&onlyPayload=false") ~>
        wrapWithRejectionHandler {
          configurationsService.querySingleRoute
        } ~> check {
        rejection.isInstanceOf[MalformedQueryParamRejection]
      }
    }

    "Agora" should "supply a default methodConfigVersion of 1 if it's missing" in {

      val url = testConfigWithSnapshotMissingConfigVersion.namespace.get + "/" +
        testConfigWithSnapshotMissingConfigVersion.name.get + "/" +
        testConfigWithSnapshotMissingConfigVersion.snapshotId.get

      Get(baseURL + "?payloadAsObject=true") ~>
      configurationsService.querySingleRoute ~> check {
        assert(status == OK)

        val entity = responseAs[AgoraEntity]

        assert(entity.payloadObject.get.methodConfigVersion == 1)
      }
    }

  }

}
