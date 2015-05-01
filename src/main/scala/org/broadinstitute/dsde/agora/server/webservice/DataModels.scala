package org.broadinstitute.dsde.agora.server.webservice

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(value = "Methods Query Response")
case class MethodsQueryResponse(
                                 @(ApiModelProperty@field)(required = true, value = "The ID of this method")
                               id: String,
                                 @(ApiModelProperty@field)(required = true, value = "The files associated with this method, each with a unique user-supplied string key.")
                               files: Map[String, String],
                                 @(ApiModelProperty@field)(required = true, value = "The metadata key-value pairs associated with this method.")
                               metadata: Map[String,String]) 

