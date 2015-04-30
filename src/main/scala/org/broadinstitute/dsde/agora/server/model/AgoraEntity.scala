
package org.broadinstitute.dsde.agora.server.model

import java.util.Date

import com.novus.salat._
import com.novus.salat.global._
import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import spray.http.HttpEntity
import spray.http.MediaTypes._
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.Unmarshaller

import scala.annotation.meta.field

object AgoraEntity {
  implicit val AgoraEntityUnmarshaller =
    Unmarshaller[AgoraEntity](`application/json`) {
      case HttpEntity.NonEmpty(contentType, data) ⇒ grater[AgoraEntity].fromJSON(data.asString)
      case HttpEntity.Empty ⇒ new AgoraEntity()
    }

  implicit val AgoraEntityMarshaller =
    Marshaller.of[AgoraEntity](`application/json`) { (value, contentType, context) =>
      context.marshalTo(HttpEntity(contentType, grater[AgoraEntity].toCompactJSON(value)))
    }
}

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
