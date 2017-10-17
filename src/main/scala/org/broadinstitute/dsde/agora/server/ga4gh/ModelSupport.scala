package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.ga4gh.Models.{Metadata, Tool, ToolClass, ToolDescriptor, ToolDescriptorType, ToolId, ToolVersion}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, MethodDefinition}
import wdl4s.WdlNamespaceWithWorkflow

import scala.util.Success

/**
  * Support class to handle apply methods with some degree of business logic
  */
object ModelSupport {

  final val ORGANIZATION = "The Broad Institute or Harvard and MIT"
  final val VERIFIED_SOURCE = ""

  def toolIdFromEntity(entity:AgoraEntity): ToolId = {
    assert(entity.namespace.nonEmpty, "cannot create a ToolId if entity namespace is empty")
    assert(entity.name.nonEmpty, "cannot create a ToolId if entity name is empty")
    new ToolId(entity.namespace.get, entity.name.get)
  }

  def toolIdFromMethod(method:MethodDefinition): ToolId = {
    assert(method.namespace.nonEmpty, "cannot create a ToolId if method definition namespace is empty")
    assert(method.name.nonEmpty, "cannot create a ToolId if method definition name is empty")
    new ToolId(method.namespace.get, method.name.get)
  }

  def toolClassFromEntityType(entityType: Option[AgoraEntityType.EntityType]): ToolClass = {
    assert(entityType.nonEmpty, "cannot create a ToolClass if entity type is empty")
    val str = entityType.get.toString
    new ToolClass(str, str, "")
  }

  def toolFromEntities(entities:Seq[AgoraEntity]): Tool = {
    val representative = entities.last
    val versions = entities.toList map (x => ModelSupport.toolVersionFromEntity(x))
    val latestVersion = versions.last
    val id = ToolId(representative).toString
    val url = AgoraConfig.GA4GH.toolUrl(id, latestVersion.id, latestVersion.`descriptor-type`.last)
    val author = findAuthorsInWdl(representative.payload)
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
      `verified-source`= VERIFIED_SOURCE,
      signed=false,
      versions=versions
    )
  }

  def toolVersionFromEntity(entity: AgoraEntity): ToolVersion = {
    assert(entity.entityType.contains(AgoraEntityType.Workflow), "cannot create a ToolVersion if entityType is not Workflow")
    assert(entity.snapshotId.nonEmpty, "cannot create a ToolVersion if snapshot id is empty")
    new ToolVersion(
      name = entity.name.getOrElse(""),
      url = entity.url.getOrElse(""),
      id = ToolId(entity).toString,
      image = "",
      `descriptor-type` = List("WDL"),
      dockerfile = false,
      `meta-version` = entity.snapshotId.getOrElse(Int.MinValue).toString,
      verified = false,
      `verified-source` = VERIFIED_SOURCE
    )
  }

  def toolDescriptorFromEntity(entity: AgoraEntity): ToolDescriptor = {
    new ToolDescriptor(
      `type` = ToolDescriptorType.WDL,
      descriptor = entity.payload.getOrElse(""),
      url = entity.url.getOrElse("")
    )
  }

  /**
    * Metadata is always a constant.
    *
    * @return Metadata
    */
  def metadata(): Metadata = {
    new Metadata(version = "1.0.0", `api-version` = "1.0.0", country = "USA", `friendly-name` = "FireCloud")
  }

  /**
   * Looks for all populated "meta: author=" fields in the optional wdl meta fields.
   */
  def findAuthorsInWdl(payload: Option[String]): String = {
    val field = "author"
    payload match {
      case Some(wdl) =>
        WdlNamespaceWithWorkflow.load(wdl, Seq.empty) match {
          case Success(parsed) =>
            val authors = parsed.tasks.map(_.meta.getOrElse(field, "")) ++ parsed.workflows.map(_.meta.getOrElse(field, ""))
            authors.filterNot(_.isEmpty).mkString(", ")
          case _ => ""
        }
      case _ => ""
    }
  }

}
