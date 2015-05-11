
package org.broadinstitute.dsde.agora.server.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import org.joda.time.DateTime

import scala.annotation.meta.field

@ApiModel(value = "Agora Method")
case class AgoraEntity(@(ApiModelProperty@field)(required = false, value = "The namespace to which the method belongs")
                       namespace: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method name ")
                       name: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method id")
                       var id: Option[Int] = None,
                       @(ApiModelProperty@field)(required = false, value = "A short description of the method")
                       synopsis: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "Method documentation")
                       documentation: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "User who owns this method in the methods repo")
                       owner: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The date the method was inserted in the methods repo")
                       createDate: Option[DateTime] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method payload")
                       payload: Option[String] = None
                        )
