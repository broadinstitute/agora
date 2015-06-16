package org.broadinstitute.dsde.agora.server.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(value = "Agora Method")
case class AgoraError(@(ApiModelProperty@field)(required = true, value = "The error message") error: String)
