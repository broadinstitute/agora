package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.ga4gh.Models.{Metadata, Tool, ToolClass, ToolDescriptor, ToolDescriptorType, ToolId, ToolVersion}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, MethodDefinition}
import wdl4s.WdlNamespaceWithWorkflow

/**
  * Support class to handle apply methods with some degree of business logic
  */
object ModelSupport {

  final val organization = "The Broad Institute of Harvard and MIT"
  final val verifiedSource = ""
  final val version = "1.0.0"
  final val apiVersion = "1.0.0"
  final val country = "USA"
  final val friendlyName = "FireCloud"

  def toolIdFromEntity(entity:AgoraEntity): ToolId = {
    assert(entity.namespace.nonEmpty, "cannot create a ToolId if entity namespace is empty")
    assert(entity.name.nonEmpty, "cannot create a ToolId if entity name is empty")
    ToolId(entity.namespace.get, entity.name.get)
  }

  def toolIdFromMethod(method:MethodDefinition): ToolId = {
    assert(method.namespace.nonEmpty, "cannot create a ToolId if method definition namespace is empty")
    assert(method.name.nonEmpty, "cannot create a ToolId if method definition name is empty")
    ToolId(method.namespace.get, method.name.get)
  }

  def toolClassFromEntityType(entityType: Option[AgoraEntityType.EntityType]): ToolClass = {
    assert(entityType.nonEmpty, "cannot create a ToolClass if entity type is empty")
    val str = entityType.get.toString
    ToolClass(str, str, "")
  }

  def toolFromEntities(entities:Seq[AgoraEntity]): Tool = {
    val representative = entities.last
    val versions = entities.toList map (x => toolVersionFromEntity(x))
    val latestVersion = versions.last
    val id = ToolId(representative).toString
    val url = AgoraConfig.GA4GH.toolUrl(id, latestVersion.id, latestVersion.`descriptor-type`.last)
    // Parse the wdl once since we need to get different things from it
    val wdl = representative.payload match {
      case x if x.isDefined => Some(WdlNamespaceWithWorkflow.load(x.get, Seq.empty).get)
      case _ => None
    }
    Tool(
      url=url,
      id=id,
      organization=organization,
      toolname=latestVersion.name,
      toolclass=ToolClass(representative),
      description=representative.synopsis.getOrElse(""),
      author=findAuthorsInWdl(wdl),
      `meta-version` = latestVersion.`meta-version`,
      contains=findContainsInWdl(wdl),
      verified=false,
      `verified-source`= verifiedSource,
      signed=false,
      versions=versions
    )
  }

  def toolVersionFromEntity(entity: AgoraEntity): ToolVersion = {
    assert(entity.entityType.contains(AgoraEntityType.Workflow), "cannot create a ToolVersion if entityType is not Workflow")
    assert(entity.snapshotId.nonEmpty, "cannot create a ToolVersion if snapshot id is empty")
    ToolVersion(
      name = entity.name.getOrElse(""),
      url = entity.url.getOrElse(""),
      id = ToolId(entity).toString,
      image = "",
      `descriptor-type` = List("WDL"),
      dockerfile = false,
      `meta-version` = entity.snapshotId.getOrElse(Int.MinValue).toString,
      verified = false,
      `verified-source` = verifiedSource
    )
  }

  def toolDescriptorFromEntity(entity: AgoraEntity): ToolDescriptor = {
    val id = ToolId(entity).toString
    val version = toolVersionFromEntity(entity)
    ToolDescriptor(
      `type` = ToolDescriptorType.WDL,
      descriptor = entity.payload.getOrElse(""),
      url = AgoraConfig.GA4GH.toolUrl(id, version.id, version.`descriptor-type`.last)
    )
  }

  /**
    * Metadata is always a constant.
    */
  def metadata(): Metadata = {
    new Metadata(version = version, `api-version` = apiVersion, country = country, `friendly-name` = friendlyName)
  }

  /**
   * Looks for all populated "meta: author=X" and "meta: email=Y" fields in the optional wdl meta fields.
   */
  def findAuthorsInWdl(wdl: Option[WdlNamespaceWithWorkflow]): String = {
    val authorField = "author"
    val emailField = "email"
    val stringFormat = "%s <%s>"
    val authors: List[String] = wdl match {
      case Some(parsed) =>
        parsed.tasks.map { task =>
          formatAuthorEmail(task.meta.getOrElse(emailField,""), task.meta.getOrElse(authorField,""))
        }.toList ++ parsed.workflows.map { workflow =>
          formatAuthorEmail(workflow.meta.getOrElse(emailField,""), workflow.meta.getOrElse(authorField,""))
        }
      case _ => List.empty[String]
    }
    authors.filterNot(_.isEmpty).distinct.mkString(", ")
  }

  private def formatAuthorEmail(email: String, author: String): String = {
    val stringFormat = "%s <%s>"
    (email.trim.isEmpty, author.trim.isEmpty) match {
      case (true, false) => author.trim
      case (false, true) => email.trim
      case (false, false) => stringFormat.format(author.trim, email.trim)
      case _ => ""
    }
  }

  /**
   * Looks for all task and workflow names in wdl
   */
  def findContainsInWdl(wdl: Option[WdlNamespaceWithWorkflow]): List[String] = {
    wdl match {
      case Some(parsed) => parsed.tasks.map(_.fullyQualifiedName).toList ++ parsed.workflows.map(_.fullyQualifiedName)
      case _ => List.empty[String]
    }
  }

}
