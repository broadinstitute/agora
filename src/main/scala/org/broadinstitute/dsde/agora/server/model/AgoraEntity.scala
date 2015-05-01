
package org.broadinstitute.dsde.agora.server.model

import java.util.Date

import com.wordnik.swagger.annotations.{ApiModelProperty, ApiModel}

import scala.annotation.meta.field

@ApiModel(value = "Methods Query Response")
case class AgoraEntity(@(ApiModelProperty@field)(required = true, value = "The method id")
                       var id: Option[Int] = None,
                       @(ApiModelProperty@field)(required = true, value = "The namespace to which the method belongs")
                       namespace: Option[String] = None,
                       @(ApiModelProperty@field)(required = true, value = "The method name ")
                       name: Option[String] = None,
                       @(ApiModelProperty@field)(required = true, value = "A short description of the method")
                       synopsis: Option[String] = None,
                       @(ApiModelProperty@field)(required = true, value = "Method documentation")
                       documentation: Option[String] = None,
                       @(ApiModelProperty@field)(required = true, value = "User who owns this method in the methods repo")
                       owner: Option[String] = None,
                       @(ApiModelProperty@field)(required = true, value = "The date the method was inserted in the methods repo")
                       createDate: Option[Date] = None,
                       @(ApiModelProperty@field)(required = true, value = "The method payload")
                       payload: Option[String] = None
                        )
