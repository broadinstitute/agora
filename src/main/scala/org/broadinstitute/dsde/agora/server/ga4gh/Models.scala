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
    def apply(entity:AgoraEntity) = {
      assert(entity.namespace.nonEmpty, "cannot create a ToolId if entity namespace is empty")
      assert(entity.name.nonEmpty, "cannot create a ToolId if entity name is empty")
      new ToolId(entity.namespace.get, entity.name.get)
    }
    def apply(method:MethodDefinition) = {
      assert(method.namespace.nonEmpty, "cannot create a ToolId if method definition namespace is empty")
      assert(method.name.nonEmpty, "cannot create a ToolId if method definition name is empty")
      new ToolId(method.namespace.get, method.name.get)
    }
  }
  case class ToolClass(id: String, name: String, description: String)
  object ToolClass {
    def apply(entity:AgoraEntity) = fromEntityType(entity.entityType)
    def apply(method:MethodDefinition) = fromEntityType(method.entityType)
    def fromEntityType(entityType: Option[AgoraEntityType.EntityType]) = {
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
    metaVersion: String,
    contains: List[String],
    verified: Boolean,
    verifiedSource: String,
    signed: Boolean,
    versions: List[ToolVersion])

  object Tool {
    def apply(entities:Seq[AgoraEntity]) = {
      val representative = entities.last
      new Tool(
        url="", // TODO
        id=ToolId(representative).toString,
        organization="",
        toolname="", // TODO
        toolclass=ToolClass(representative),
        description="", // TODO: should we use a snapshot synopsis or description?
        author="", // TODO: can we get this from the WDL?
        metaVersion="", // TODO
        contains=List.empty[String],
        verified=false,
        verifiedSource="",
        signed=false,
        versions=entities.toList map (x => ToolVersion(x))
      )
    }
  }

  case class ToolVersion(
    name: String,
    url: String,
    id: String,
    image: String,
    descriptorType: List[String],
    dockerfile: Boolean,
    metaVersion: String,
    verified: Boolean,
    verifiedSource: String)

  object ToolVersion{
    def apply(entity: AgoraEntity) = {
      assert(entity.entityType.contains(AgoraEntityType.Workflow), "cannot create a ToolVersion if entityType is not Workflow")
      assert(entity.snapshotId.nonEmpty, "cannot create a ToolVersion if snapshot id is empty")
      new ToolVersion(
        name = entity.name.getOrElse(""),
        url = entity.url.getOrElse(""),
        id = ToolId(entity).toString,
        image = "",
        descriptorType = List("WDL"),
        dockerfile = false, // TODO
        metaVersion = entity.snapshotId.getOrElse(Int.MinValue).toString,
        verified = false,
        verifiedSource = ""
      )
    }
  }

  case class ToolDescriptor (
    url: String,
    descriptor: String ,
    `type`: ToolDescriptorType.DescriptorType)

  object ToolDescriptorType extends Enumeration {
    type DescriptorType = Value
    val WDL = Value("WDL")
    val PLAIN_WDL = Value("plain-WDL")
    val CWL = Value("CWL")
    val PLAIN_CWL = Value("plain-CWL")
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

  implicit val toolIdFormat=jsonFormat2(ToolId.apply)
  implicit val toolClassFormat=jsonFormat3(ToolClass.apply)
  implicit object DescriptorTypeFormat extends RootJsonFormat[ToolDescriptorType.DescriptorType] {
    override def write(obj: ToolDescriptorType.DescriptorType): JsValue = JsString(obj.toString)

    override def read(value: JsValue): ToolDescriptorType.DescriptorType = value match {
      case JsString(name) => ToolDescriptorType.withName(name)
      case _ => throw new DeserializationException("only string supported")
    }
  }
  implicit val ToolDescriptorFormat = jsonFormat3(ToolDescriptor)
  implicit val toolVersionFormat=jsonFormat9(ToolVersion.apply)
  implicit val toolFormat=jsonFormat13(Tool.apply)
  implicit val toolDockerfileFormat = jsonFormat2(ToolDockerfile)
  implicit val MetadataFormat = jsonFormat4(Metadata)

  // TODO: ensure json formats read/write keys according to the ga4gh model specs
}
