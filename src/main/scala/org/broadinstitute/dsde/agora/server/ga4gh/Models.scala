package org.broadinstitute.dsde.agora.server.ga4gh

import spray.json.DefaultJsonProtocol._
import spray.json.{DeserializationException, JsString, JsValue, RootJsonFormat}

object Models {

  object ToolDescriptorType extends Enumeration {
    type DescriptorType = Value
    val WDL = Value("WDL")
    val PLAIN_WDL = Value("plain-WDL")
    val CWL = Value("CWL")
    val PLAIN_CWL = Value("plain-CWL")
  }

  implicit object DescriptorTypeFormat extends RootJsonFormat[ToolDescriptorType.DescriptorType] {
    override def write(obj: ToolDescriptorType.DescriptorType): JsValue = JsString(obj.toString)

    override def read(value: JsValue): ToolDescriptorType.DescriptorType = value match {
      case JsString(name) => ToolDescriptorType.withName(name)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  case class ToolDescriptor (
    url: String,
    descriptor: String ,
    `type`: ToolDescriptorType.DescriptorType)

  implicit val ToolDescriptorFormat = jsonFormat3(ToolDescriptor)

  case class ToolId(namespace: String, name: String)

}
