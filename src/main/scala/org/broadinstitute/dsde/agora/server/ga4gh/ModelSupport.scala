package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.ga4gh.Models.{Metadata, Tool, ToolClass, ToolDescriptor, ToolDescriptorType, ToolId, ToolVersion}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, MethodDefinition}

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

  // This is used only from unauthenticated endpoint to get all public methods in a GA4GH format.
  // This format includes an 'author' that we attempt to get by parsing the WDL of each method
  // that is returned.  There is no pagination on this method.  In production, because of this
  // parsing, the method times out and returns a 503.  It has been called 11 times in the previous
  // 3 months and never returned successfully.  This method is currently broken, and spending
  // effort to fix it is out of scope for WDL 1.0.  In light of the low usage and 100% failure
  // rate in production with no complaints raises questions about it's value overall.  After
  // discussion with product, we will not parse this field from the WDL any longer but
  // will still return the field to be syntactically compliant.
  def toolFromEntities(entities:Seq[AgoraEntity]): Tool = {
    val representative = entities.last
    val versions = entities.toList map (x => toolVersionFromEntity(x))
    val latestVersion = versions.last
    val id = ToolId(representative).toString
    val url = AgoraConfig.GA4GH.toolUrl(id, latestVersion.id, latestVersion.`descriptor-type`.last)
    Tool(
      url=url,
      id=id,
      organization=organization,
      toolname=latestVersion.name,
      toolclass=ToolClass(representative),
      description=representative.synopsis.getOrElse(""),
      author="",
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
    Metadata(version = version, `api-version` = apiVersion, country = country, `friendly-name` = friendlyName)
  }

}
