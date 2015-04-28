package org.broadinstitute.dsde.agora.server.webservice

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(value = "Tasks Query Response")
case class TasksQueryResponse(
                               @(ApiModelProperty@field)(required = true, value = "The ID of this task/workflow")
                               id: String,
                               @(ApiModelProperty@field)(required = true, value = "The files associated with this task/workflow, each with a unique user-supplied string key.")
                               files: Map[String, String],
                               @(ApiModelProperty@field)(required = true, value = "The metadata key-value pairs associated with this task/workflow.")
                               metadata: Map[String,String]) 

