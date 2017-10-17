package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, MethodDefinition}
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}
import wdl4s.WdlNamespaceWithWorkflow

import scala.util.Success

object Models {

  final val ID_DELIMITER = ":"

  case class ToolId(namespace: String, name: String) {
    override def toString: String = s"$namespace$ID_DELIMITER$name"
  }
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
  case class ToolClass(id: String, name: String, description: String)
  object ToolClass {
    def apply(entity:AgoraEntity): ToolClass = fromEntityType(entity.entityType)
    def apply(method:MethodDefinition): ToolClass = fromEntityType(method.entityType)
    def fromEntityType(entityType: Option[AgoraEntityType.EntityType]): ToolClass = {
      assert(entityType.nonEmpty, "cannot create a ToolClass if entity type is empty")
      val str = entityType.get.toString
      new ToolClass(str, str, "")
    }
  }

  case class Tool(
    url:String,
    id: String,
    organization: String,
    toolname: String,
    toolclass: ToolClass,
    description: String,
    author: String,
    `meta-version`: String,
    contains: List[String],
    verified: Boolean,
    `verified-source`: String,
    signed: Boolean,
    versions: List[ToolVersion])

  object Tool {
    def apply(entities:Seq[AgoraEntity]): Tool = {
      val representative = entities.last
      val versions = entities.toList map (x => ToolVersion(x))
      val latestVersion = versions.last
      val id = ToolId(representative).toString
      val url = AgoraConfig.GA4GH.toolUrl(id, latestVersion.id, latestVersion.`descriptor-type`.last)
      val author = findAuthorInWdl(representative.payload)
      new Tool(
        url=url,
        id=id,
        organization="", // TODO: is this always the Broad?
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

  case class ToolVersion(
    name: String,
    url: String,
    id: String,
    image: String,
    `descriptor-type`: List[String],
    dockerfile: Boolean,
    `meta-version`: String,
    verified: Boolean,
    `verified-source`: String)

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

  case class ToolDescriptor (
    url: String,
    descriptor: String ,
    `type`: ToolDescriptorType.DescriptorType)

  object ToolDescriptorType extends Enumeration {
    type DescriptorType = Value
    val WDL: ToolDescriptorType.Value = Value("WDL")
    val PLAIN_WDL: ToolDescriptorType.Value = Value("plain-WDL")
    val CWL: ToolDescriptorType.Value = Value("CWL")
    val PLAIN_CWL: ToolDescriptorType.Value = Value("plain-CWL")
  }

  // ToolTests

  case class ToolDockerfile(
    dockerfile: String,
    url: String)

  case class Metadata(
    version: String,
    apiVersion: String,
    country: String,
    friendlyName: String)

  implicit val toolIdFormat: RootJsonFormat[ToolId] = jsonFormat2(ToolId.apply)
  implicit val toolClassFormat: RootJsonFormat[ToolClass] = jsonFormat3(ToolClass.apply)
  implicit object DescriptorTypeFormat extends RootJsonFormat[ToolDescriptorType.DescriptorType] {
    override def write(obj: ToolDescriptorType.DescriptorType): JsValue = JsString(obj.toString)

    override def read(value: JsValue): ToolDescriptorType.DescriptorType = value match {
      case JsString(name) => ToolDescriptorType.withName(name)
      case _ => throw DeserializationException("only string supported")
    }
  }
  implicit val ToolDescriptorFormat: RootJsonFormat[ToolDescriptor] = jsonFormat3(ToolDescriptor)
  implicit val toolVersionFormat: RootJsonFormat[ToolVersion] =jsonFormat9(ToolVersion.apply)
  implicit val toolFormat: RootJsonFormat[Tool] =jsonFormat13(Tool.apply)
  implicit val toolDockerfileFormat: RootJsonFormat[ToolDockerfile] = jsonFormat2(ToolDockerfile)
  implicit val MetadataFormat: RootJsonFormat[Metadata] = jsonFormat4(Metadata)

  // TODO: ensure json formats read/write keys according to the ga4gh model specs

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
