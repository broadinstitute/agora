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

  final val organization = "The Broad Institute or Harvard and MIT"
  final val verifiedSource = ""
  final val version = "1.0.0"
  final val apiVersion = version
  final val country = "USA"
  final val friendlyName = "FireCloud"

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
      organization=organization,
      toolname=latestVersion.name,
      toolclass=ToolClass(representative),
      description=representative.synopsis.getOrElse(""),
      author=author,
      `meta-version` = latestVersion.`meta-version`,
      contains=List.empty[String],
      verified=false,
      `verified-source`= verifiedSource,
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
      `verified-source` = verifiedSource
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
    */
  def metadata(): Metadata = {
    new Metadata(version = version, `api-version` = apiVersion, country = country, `friendly-name` = friendlyName)
  }

  /**
   * Looks for all populated "meta: author=X" and "meta: email=Y" fields in the optional wdl meta fields.
   */
  def findAuthorsInWdl(payload: Option[String]): String = {
    val authorField = "author"
    val emailField = "email"
    val stringFormat = "%s <%s>"
    payload match {
      case Some(wdl) =>
        WdlNamespaceWithWorkflow.load(wdl, Seq.empty) match {
          case Success(parsed) =>
            val authorEmails: Seq[(String, String)] = {
              parsed.tasks.map(_.meta.getOrElse(authorField, "")).zip(parsed.tasks.map(_.meta.getOrElse(emailField, ""))) ++
                parsed.workflows.map(_.meta.getOrElse(authorField, "")).zip(parsed.workflows.map(_.meta.getOrElse(emailField, "")))
            }.filterNot(_._1.isEmpty)
            authorEmails map Function.tupled{ (author: String, email: String) =>
              if (email.isEmpty) author
              else stringFormat.format(author, email)
            } mkString ", "
          case _ => ""
        }
      case _ => ""
    }
  }

}
