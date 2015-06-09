
package org.broadinstitute.dsde.agora.server.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraProjectionDefaults._
import org.joda.time.DateTime

import scala.annotation.meta.field

object AgoraEntityType extends Enumeration {
  def byPath(path: String): Seq[EntityType] = path match {
    case AgoraConfig.methodsRoute => Seq(Task, Workflow)
    case AgoraConfig.configurationsRoute => Seq(Configuration)
  }

  type EntityType = Value
  val Task = Value("Task")
  val Workflow = Value("Workflow")
  val Configuration = Value("Configuration")
}

@ApiModel(value = "Agora Method")
case class AgoraEntity(@(ApiModelProperty@field)(required = false, value = "The namespace to which the method belongs")
                       namespace: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method name ")
                       name: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method snapshot id")
                       snapshotId: Option[Int] = None,
                       @(ApiModelProperty@field)(required = false, value = "A short description of the method")
                       synopsis: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "Method documentation")
                       documentation: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "User who owns this method in the methods repo")
                       owner: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The date the method was inserted in the methods repo")
                       createDate: Option[DateTime] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method payload")
                       payload: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "URI for method details")
                       url: Option[String] = None,
                       entityType: Option[AgoraEntityType.EntityType]= None
                        )

object AgoraProjectionDefaults {
  val RequiredProjectionFields = Seq[String]("namespace", "name", "snapshotId")
}

case class AgoraEntityProjection(includedFields: Seq[String], excludedFields: Seq[String]) {
  require(excludedFields.intersect(RequiredProjectionFields).isEmpty)
  require(includedFields.isEmpty || excludedFields.isEmpty)

  def totalFields = includedFields.size + excludedFields.size
}