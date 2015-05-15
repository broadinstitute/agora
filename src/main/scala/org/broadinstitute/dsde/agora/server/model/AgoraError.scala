package org.broadinstitute.dsde.agora.server.model

import com.wordnik.swagger.annotations.{ApiModelProperty, ApiModel}

import scala.annotation.meta.field

/**
 * Created by dshiga on 5/15/15.
 */
@ApiModel(value = "Agora Method")
case class AgoraError(@(ApiModelProperty@field)(required = true, value = "The error message")
                       error: String
                        )
