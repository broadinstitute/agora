
package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraEntityPermissionsClient, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.DoNotDiscover
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.ValidationRejection

@DoNotDiscover
class AgoraConfigurationsSpec extends ApiServiceSpec {

  var workflow1: AgoraEntity = _
  var workflow2: AgoraEntity = _
  var workflow3: AgoraEntity = _
  var testEntityToBeRedacted2WithId: AgoraEntity = _
  var testConfigToBeRedactedWithId: AgoraEntity = _

  override def beforeAll() = {
    ensureDatabasesAreRunning()
    workflow1 = agoraBusiness.insert(testWorkflow1, mockAutheticatedOwner.get)
    workflow2 = agoraBusiness.insert(testWorkflow2, mockAutheticatedOwner.get)
    workflow3 = agoraBusiness.insert(testWorkflow3, mockAutheticatedOwner.get)
    testEntityToBeRedacted2WithId = agoraBusiness.insert(testWorkflowToBeRedacted, mockAutheticatedOwner.get)
    testConfigToBeRedactedWithId = agoraBusiness.insert(testConfigToBeRedacted, mockAutheticatedOwner.get)
    agoraBusiness.insert(testTask2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testWorkflow2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testConfig1, mockAutheticatedOwner.get)
    agoraBusiness.insert(testConfig2, mockAutheticatedOwner.get)
    agoraBusiness.insert(testConfig3, mockAutheticatedOwner.get)
  }

  override def afterAll() = {
    clearDatabases()
  }

  "Agora" should "be able to store a task configuration" in {
    Post(ApiUtil.Configurations.withLeadingVersion, testConfig3) ~>
      configurationsService.postRoute ~> check {

      val referencedWorkflow = AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(
        workflow1.namespace.get,
        workflow1.name.get,
        workflow1.snapshotId.get)

      handleError(entity.as[AgoraEntity], (entity: AgoraEntity) => {
        assert(entity.namespace === config3Namespace)
        assert(entity.name === config3Name)
        assert(entity.synopsis === synopsis3)
        assert(entity.documentation === documentation1)
        assert(entity.owner === owner1)
        assert(entity.payload === taskConfigPayload)
        assert(entity.snapshotId !== None)
        assert(entity.createDate !== None)
        assert(referencedWorkflow.id !== None)
        assert(entity.method !== None)

        val foundMethod = entity.method.get
        assert(foundMethod.namespace === namespace1)
        assert(foundMethod.name === name1)
        assert(foundMethod.snapshotId === snapshotId1)
        assert(foundMethod.url !== None)
      })
    }
  }

  "Agora" should "not allow you to add a configuration that references a task" in {
    Post(ApiUtil.Configurations.withLeadingVersion, testBadConfigReferencesTask) ~>
      configurationsService.postRoute ~> check {
      assert(status === BadRequest)
    }
  }

  "Agora" should "populate method references when returning configurations" in {
    Get(ApiUtil.Configurations.withLeadingVersion) ~>
      configurationsService.queryRoute ~> check {

      handleError(entity.as[Seq[AgoraEntity]], (configs: Seq[AgoraEntity]) => {
        val workflow2 = AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(namespace2.get, name1.get, snapshotId1.get)
        val workflow3 = AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(namespace1.get, name2.get, snapshotId1.get)

        val config1 = AgoraDao.createAgoraDao(Seq(AgoraEntityType.Configuration)).findSingle(
          testConfig1.namespace.get, testConfig1.name.get, 2)
        val config2 = AgoraDao.createAgoraDao(Seq(AgoraEntityType.Configuration)).findSingle(
          testConfig2.namespace.get, testConfig2.name.get, 1)
        val config3 = AgoraDao.createAgoraDao(Seq(AgoraEntityType.Configuration)).findSingle(
          testConfig3.namespace.get, testConfig3.name.get, 1)

        val foundConfig1 = configs.find(config => namespaceNameIdMatch(config, config1)).get
        val foundConfig2 = configs.find(config => namespaceNameIdMatch(config, config2)).get
        val foundConfig3 = configs.find(config => namespaceNameIdMatch(config, config3)).get
        
        val workflowRef1 = foundConfig1.method.get
        val workflowRef2 = foundConfig2.method.get
        val workflowRef3 = foundConfig3.method.get

        assert(workflowRef1.namespace !== None)
        assert(workflowRef1.name !== None)
        assert(workflowRef1.snapshotId !== None)
        assert(workflowRef2.namespace !== None)
        assert(workflowRef2.name !== None)
        assert(workflowRef2.snapshotId !== None)
        assert(workflowRef3.namespace !== None)
        assert(workflowRef3.name !== None)
        assert(workflowRef3.snapshotId !== None)

        assert(workflowRef1.namespace === workflow1.namespace)
        assert(workflowRef1.name === workflow1.name)
        assert(workflowRef1.snapshotId === workflow1.snapshotId)
        assert(workflowRef2.namespace === workflow2.namespace)
        assert(workflowRef2.name === workflow2.name)
        assert(workflowRef2.snapshotId === workflow2.snapshotId)
        assert(workflowRef3.namespace === workflow1.namespace)
        assert(workflowRef3.name === workflow1.name)
        assert(workflowRef3.snapshotId === workflow1.snapshotId)
      })

    }
  }

  "Agora" should "not allow you to post a new configuration if you don't have permission to read the method that it references" in {
    val noPermission = new AccessControl(AgoraConfig.mockAuthenticatedUserEmail, AgoraPermissions(AgoraPermissions.Nothing))
    AgoraEntityPermissionsClient.editEntityPermission(workflow1, noPermission)
    Post(ApiUtil.Configurations.withLeadingVersion, testConfig3) ~>
      configurationsService.postRoute ~> check {
        assert(status === NotFound)
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
      rejection === ValidationRejection
    }
  }

  "Agora" should "redact methods when it has at least 1 associated configuration" in {
    Delete(ApiUtil.Methods.withLeadingVersion + "/" +
      testEntityToBeRedacted2WithId.namespace.get + "/" +
      testEntityToBeRedacted2WithId.name.get + "/" +
      testEntityToBeRedacted2WithId.snapshotId.get) ~>
    methodsService.querySingleRoute ~>
    check {
      assert(body.asString === "1")
    }
  }

  "Agora" should "redact associated configurations when the referenced method is redacted" in {
    Get(ApiUtil.Configurations.withLeadingVersion + "/" +
      testConfigToBeRedactedWithId.namespace.get + "/" +
      testConfigToBeRedactedWithId.name.get + "/" +
      testConfigToBeRedactedWithId.snapshotId.get) ~>
    configurationsService.querySingleRoute ~>
    check {
      assert(body.asString contains "not found")
    }
  }
}
