package org.broadinstitute.dsde.rawls.model

import org.joda.time.DateTime

case class MethodRepoMethod(
                   methodNamespace: String,
                   methodName: String,
                   methodVersion: Int
                   )

case class MethodConfiguration(
                   namespace: String,
                   name: String,
                   rootEntityType: String,
                   prerequisites: Map[String, AttributeString],
                   inputs: Map[String, AttributeString],
                   outputs: Map[String, AttributeString],
                   methodRepoMethod: MethodRepoMethod,
                   methodConfigVersion: Int = 1,
                   deleted: Boolean = false,
                   deletedDate: Option[DateTime] = None
                   ) {
}

sealed trait Attribute
sealed trait AttributeListElementable extends Attribute //terrible name for "this type can legally go in an attribute list"
sealed trait AttributeValue extends AttributeListElementable
case class AttributeString(val value: String) extends AttributeValue

class WorkspaceJsonSupport extends JsonSupport {
  import spray.json.DefaultJsonProtocol._

  implicit val MethodStoreMethodFormat = jsonFormat3(MethodRepoMethod)

  implicit val MethodConfigurationFormat = jsonFormat10(MethodConfiguration)
}

object WorkspaceJsonSupport extends WorkspaceJsonSupport
