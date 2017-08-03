package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class AgoraEntityTest extends FlatSpec {

  "Agora" should "create not throw exceptions on valid a valid entity" in {
    assert(AgoraEntity(namespace1, name1, snapshotId1, synopsis1) === AgoraEntity(namespace1, name1, snapshotId1, synopsis1))
  }

  "Agora" should "validate agoraEntity namespace is not empty" in {
    val ex = intercept[IllegalArgumentException] {
      val entity = AgoraEntity(badNamespace)
      AgoraEntity.getErrors(entity)
    }
    assert(ex.getMessage contains "Namespace")
  }

  "Agora" should "validate agoraEntity name is not empty" in {
    val ex = intercept[IllegalArgumentException] {
      val entity = AgoraEntity(name = badName)
      AgoraEntity.getErrors(entity)
    }
    assert(ex.getMessage contains "Name")
  }

  "Agora" should "validate agoraEntity snapshotId is greater than 0" in {
    val ex = intercept[IllegalArgumentException] {
      val entity = AgoraEntity(snapshotId = badId)
      AgoraEntity.getErrors(entity)
    }
    assert(ex.getMessage contains "SnapshotId")
  }


  "Agora" should "validate agoraEntity synopsis is less than 80 chars" in {
    val ex = intercept[IllegalArgumentException] {
      val entity = AgoraEntity(synopsis = badSynopsis)
      AgoraEntity.getErrors(entity)
    }
    assert(ex.getMessage contains "Synopsis")
  }

  "Agora" should "validate agoraEntity documentation is less than 10kb" in {
    val ex = intercept[IllegalArgumentException] {
      val entity = AgoraEntity(documentation = bigDocumentation)
      AgoraEntity.getErrors(entity)
    }
    assert(ex.getMessage contains "Documentation")
  }

  "Agora" should "return all errors at once" in {
    val ex = intercept[IllegalArgumentException] {
      val entity = AgoraEntity(badNamespace, badName, badId)
      AgoraEntity.getErrors(entity)
    }
    assert(ex.getMessage contains "Namespace")
    assert(ex.getMessage contains "Name")
    assert(ex.getMessage contains "SnapshotId")
  }

  "Agora" should "return a URL given an entity with a namespace, name, and id" in {
    val entity = AgoraEntity(namespace = Option("broad"), name = Option("test"), snapshotId = Option(12), entityType = Option(AgoraEntityType.Task))
    assert(entity.agoraUrl === AgoraConfig.methodsUrl + "broad/test/12")
  }
}
