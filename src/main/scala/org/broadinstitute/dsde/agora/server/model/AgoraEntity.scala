
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
  def fromAgoraAddRequest(agoraAddRequest: AgoraAddRequest, createDate: Option[Date]) = {
    new AgoraEntity(namespace = Option(agoraAddRequest.namespace),
      name = Option(agoraAddRequest.name),
      synopsis = Option(agoraAddRequest.synopsis),
      documentation = Option(agoraAddRequest.documentation),
      owner = Option(agoraAddRequest.owner),
      createDate = createDate,
      payload = Option(agoraAddRequest.payload)
    )
  }
  
  def fromAgoraSearch(agoraSearch: AgoraSearch) = {
    new AgoraEntity(namespace = agoraSearch.namespace,
                    name = agoraSearch.name,
                    id = agoraSearch.id,
                    synopsis = agoraSearch.synopsis,
                    documentation = agoraSearch.documentation,
                    owner = agoraSearch.owner,
                    payload = agoraSearch.payload)
  }
}

object AgoraAddRequest {
  implicit val AgoraAddRequestUnmarshaller =
    Unmarshaller[AgoraAddRequest](`application/json`) {
      case HttpEntity.NonEmpty(contentType, data) ⇒ grater[AgoraAddRequest].fromJSON(data.asString)
    }

  implicit val AgoraAddRequestMarshaller =
    Marshaller.of[AgoraAddRequest](`application/json`) { (value, contentType, context) =>
      context.marshalTo(HttpEntity(contentType, grater[AgoraAddRequest].toCompactJSON(value)))
    }  
}

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
                       createDate: Option[Date] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method payload")
                       payload: Option[String] = None
                        )

@ApiModel(value = "Agora Search")
case class AgoraSearch(@(ApiModelProperty@field)(required = false, value = "The namespace to which the method belongs")
                       namespace: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method name ")
                       name: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method id")
                       var id: Option[Int] = None,
                       @(ApiModelProperty@field)(required = false, value = "User who owns this method in the methods repo")
                       owner: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "A short description of the method")
                       synopsis: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "Method documentation")
                       documentation: Option[String] = None,
                       @(ApiModelProperty@field)(required = false, value = "The method payload")
                       payload: Option[String] = None
                      )

@ApiModel(value = "Request to add method")
case class AgoraAddRequest(@(ApiModelProperty@field)(required = true, value = "The namespace to which the method belongs")
                           namespace: String,
                           @(ApiModelProperty@field)(required = true, value = "The method name ")
                           name: String,
                           @(ApiModelProperty@field)(required = true, value = "A short description of the method")
                           synopsis: String,
                           @(ApiModelProperty@field)(required = true, value = "Method documentation")
                           documentation: String,
                           @(ApiModelProperty@field)(required = true, value = "User who owns this method in the methods repo")
                           owner: String, // TODO: remove (use authenticated user)
                           @(ApiModelProperty@field)(required = true, value = "The method payload")
                           payload: String
                            )
