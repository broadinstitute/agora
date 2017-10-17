package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, MethodDefinition}
import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}

object Models {

  final val ID_DELIMITER = ":"

  case class ToolId(namespace: String, name: String) {
    override def toString: String = s"$namespace$ID_DELIMITER$name"
  }
  object ToolId {
    def apply(entity:AgoraEntity): ToolId = ModelSupport.toolIdFromEntity(entity)
    def apply(method:MethodDefinition): ToolId = ModelSupport.toolIdFromMethod(method)
  }

  case class ToolClass(id: String, name: String, description: String)
  object ToolClass {
    def apply(entity:AgoraEntity): ToolClass = ModelSupport.toolClassFromEntityType(entity.entityType)
    def apply(method:MethodDefinition): ToolClass = ModelSupport.toolClassFromEntityType(method.entityType)
    def apply(entityType:Some[AgoraEntityType.EntityType]): ToolClass =  ModelSupport.toolClassFromEntityType(entityType)
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
    def apply(entities:Seq[AgoraEntity]): Tool = ModelSupport.toolFromEntities(entities)
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
    def apply(entity: AgoraEntity): ToolVersion = ModelSupport.toolVersionFromEntity(entity)
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
  implicit val MetadataFormat: RootJsonFormat[Metadata] = jsonFormat4(Metadata)

  // TODO: ensure json formats read/write keys according to the ga4gh model specs

}
