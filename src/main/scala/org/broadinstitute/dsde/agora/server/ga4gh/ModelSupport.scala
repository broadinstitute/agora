package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.ga4gh.Models.{Tool, ToolClass, ToolId, ToolVersion}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, MethodDefinition}
import wdl4s.WdlNamespaceWithWorkflow

import scala.util.Success

/**
  * Support class to handle apply methods with some degree of business logic
  */
object ModelSupport {

  final val ORGANIZATION = "The Broad Institute or Harvard and MIT"


  object ToolId {
    def apply(entity:AgoraEntity): ToolId = {
      assert(entity.namespace.nonEmpty, "cannot create a ToolId if entity namespace is empty")
      assert(entity.name.nonEmpty, "cannot create a ToolId if entity name is empty")
      new ToolId(entity.namespace.get, entity.name.get)
    }
    def apply(method:MethodDefinition): ToolId = {
      assert(method.namespace.nonEmpty, "cannot create a ToolId if method definition namespace is empty")
      assert(method.name.nonEmpty, "cannot create a ToolId if method definition name is empty")
      new ToolId(method.namespace.get, method.name.get)
    }
  }


  object ToolClass {
    def apply(entity:AgoraEntity): ToolClass = apply(entity.entityType)
    def apply(method:MethodDefinition): ToolClass = apply(method.entityType)
    def apply(entityType: Option[AgoraEntityType.EntityType]): ToolClass = {
      assert(entityType.nonEmpty, "cannot create a ToolClass if entity type is empty")
      val str = entityType.get.toString
      new ToolClass(str, str, "")
    }
  }


  object Tool {
    def apply(entities:Seq[AgoraEntity]): Tool = {
      val representative = entities.last
      val versions = entities.toList map (x => ModelSupport.ToolVersion(x))
      val latestVersion = versions.last
      val id = ToolId(representative).toString
      val url = AgoraConfig.GA4GH.toolUrl(id, latestVersion.id, latestVersion.`descriptor-type`.last)
      val author = findAuthorInWdl(representative.payload)
      new Tool(
        url=url,
        id=id,
        organization=ORGANIZATION,
        toolname=latestVersion.name,
        toolclass=ToolClass(representative),
        description=representative.synopsis.getOrElse(""),
        author=author,
        `meta-version` = latestVersion.`meta-version`,
        contains=List.empty[String],
        verified=false,
        `verified-source`="",
        signed=false,
        versions=versions
      )
    }
  }


  object ToolVersion{
    def apply(entity: AgoraEntity): ToolVersion = {
      assert(entity.entityType.contains(AgoraEntityType.Workflow), "cannot create a ToolVersion if entityType is not Workflow")
      assert(entity.snapshotId.nonEmpty, "cannot create a ToolVersion if snapshot id is empty")
      new ToolVersion(
        name = entity.name.getOrElse(""),
        url = entity.url.getOrElse(""),
        id = ToolId(entity).toString,
        image = "",
        `descriptor-type` = List("WDL"),
        dockerfile = false, // TODO
        `meta-version` = entity.snapshotId.getOrElse(Int.MinValue).toString,
        verified = false,
        `verified-source` = ""
      )
    }
  }


  // Looks for the last populated "meta: author" field in the wdl meta data
  private def findAuthorInWdl(payload: Option[String]): String = {
    val field = "author"
    payload match {
      case Some(wdl) =>
        WdlNamespaceWithWorkflow.load(wdl, Seq.empty) match {
          case Success(fullWdl) =>
            val authors = fullWdl.tasks.map(_.meta.getOrElse(field, "")) ++ fullWdl.workflows.map(_.meta.getOrElse(field, ""))
            authors.filterNot(_.isEmpty).mkString(", ")
          case _ => ""
        }
      case _ => ""
    }
  }

}
