package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.scalatest.{DoNotDiscover, FreeSpec}

@DoNotDiscover
class Ga4ghModelTest extends FreeSpec {

  "GA4GH model classes" - {
    "ToolId" - {
      "should error with empty namespace" in {
        val entity = AgoraEntity(None, Some("name"))
        intercept[AssertionError] {
          ToolId(entity)
        }
        val method = MethodDefinition(name = Some("name"), numConfigurations = 0, numSnapshots = 0)
        intercept[AssertionError] {
          ToolId(method)
        }
      }
      "should error with empty name" in {
        val entity = AgoraEntity(Some("namespace"), None)
        intercept[AssertionError] {
          ToolId(entity)
        }
        val method = MethodDefinition(namespace = Some("namespace"), numConfigurations = 0, numSnapshots = 0)
        intercept[AssertionError] {
          ToolId(method)
        }
      }
      "should generate the right id string from AgoraEntity" in {
        val entity = AgoraEntity(Some("entityNamespace"), Some("entityName"))
        assertResult("entityNamespace:entityName") { ToolId(entity).toString }
      }
      "should generate the right id string from MethodDefinition" in {
        val method = MethodDefinition(namespace = Some("methodNamespace"), name = Some("methodName"), numConfigurations = 0, numSnapshots = 0)
        assertResult("methodNamespace:methodName") { ToolId(method).toString }
      }
    }
    "ToolClass" - {
      "should error with empty entityType" in {
        val entity = AgoraEntity()
        intercept[AssertionError] {
          ToolClass(entity)
        }
        val method = MethodDefinition(numConfigurations = 0, numSnapshots = 0)
        intercept[AssertionError] {
          ToolClass(method)
        }
      }
      "should create from AgoraEntity" in {
        val entity = AgoraEntity(entityType = Some(AgoraEntityType.Workflow))
        assertResult( ToolClass("Workflow", "Workflow", "")) { ToolClass(entity) }
      }
      "should create from MethodDefinition" in {
        val method = MethodDefinition(entityType = Some(AgoraEntityType.Workflow), numConfigurations = 0, numSnapshots = 0)
        assertResult( ToolClass("Workflow", "Workflow", "")) { ToolClass(method) }
      }
    }
    "Tool" - {
      "should create from MethodDefinition" in {
        val entities = Seq(
          AgoraEntity(
            namespace = Some("namespace"),
            name = Some("name"),
            snapshotId = Some(1),
            synopsis = Some("synopsis"),
            entityType = Some(AgoraEntityType.Workflow),
            managers = Seq("manager1","manager2")
          ),
          AgoraEntity(
            namespace = Some("namespace"),
            name = Some("name"),
            snapshotId = Some(3),
            synopsis = Some("synopsis3"),
            entityType = Some(AgoraEntityType.Workflow),
            managers = Seq("manager1","manager2")
          )
        )

        val expected = Tool(
          url = "",
          id = "namespace:name",
          organization = "",
          toolname = "",
          toolclass = ToolClass("Workflow","Workflow",""),
          description = "",
          author = "",
          metaVersion = "",
          contains = List.empty[String],
          verified = false,
          verifiedSource = "",
          signed = false,
          versions = entities.toList map (x => ToolVersion(x))
        )
        val actual = Tool(entities)
        assertResult(expected) { actual }
      }
    }
    "ToolVersion" - {

      val defaultEntity = AgoraEntity(namespace = Some("namespace"), name = Some("name"), snapshotId = Some(3), entityType = Some(AgoraEntityType.Workflow))

      "should error with empty namespace" in {
        val entity = defaultEntity.copy(namespace = None)
        intercept[AssertionError] {
          ToolVersion(entity)
        }
      }
      "should error with empty name" in {
        val entity = defaultEntity.copy(name = None)
        intercept[AssertionError] {
          ToolVersion(entity)
        }
      }
      "should error with empty entityType" in {
        val entity =defaultEntity.copy(entityType = None)
        intercept[AssertionError] {
          ToolVersion(entity)
        }
      }
      "should error with non-Workflow entityType" in {
        val entity =defaultEntity.copy(entityType = Some(AgoraEntityType.Configuration))
        intercept[AssertionError] {
          ToolVersion(entity)
        }
      }
      "should error with empty snapshot id" in {
        val entity =defaultEntity.copy(snapshotId = None)
        intercept[AssertionError] {
          ToolVersion(entity)
        }
      }
      "should create from AgoraEntity" in {
        val expected = ToolVersion(
          name = "name",
          url = "",
          id = "namespace:name",
          image = "",
          descriptorType = List("WDL"),
          dockerfile = false,
          metaVersion = "3",
          verified = false,
          verifiedSource = "")
        val actual = ToolVersion(defaultEntity)
      }
    }
    // TODO: 
    "ToolDescriptor" - {}
    "ToolDockerfile" - {}
    "Metadata" - {}

  }
}
