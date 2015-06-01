
package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.webservice.util.AgoraOpenAMClient.UserInfoResponse
import org.broadinstitute.dsde.agora.server.webservice.validation.AgoraValidation
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import spray.json._

import scala.language.implicitConversions

object AgoraApiJsonSupport extends DefaultJsonProtocol {

  implicit def stringToDateTime(str: String): DateTime = parserISO.parseDateTime(str)

  implicit def stringToType(str: String): AgoraEntityType.EntityType = AgoraEntityType.withName(str)

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    override def write(obj: DateTime) = {
      JsString(parserISO.print(obj))
    }

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  implicit object AgoraValidationFormat extends RootJsonFormat[AgoraValidation] {
    override def write(validation: AgoraValidation) = {
      Map("error" -> validation.messages).toJson
    }

    override def read(json: JsValue): AgoraValidation = json match {
      case x: JsObject =>
        val messages = x.fields("error").convertTo[Seq[String]]
        AgoraValidation(messages)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  implicit object AgoraEntityTypeFormat extends RootJsonFormat[AgoraEntityType.EntityType] {
    override def write(obj: AgoraEntityType.EntityType): JsValue = JsString(obj.toString)

    override def read(value: JsValue): AgoraEntityType.EntityType = value match {
      case JsString(name) => AgoraEntityType.withName(name)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  private val parserISO: DateTimeFormatter = {
    ISODateTimeFormat.dateTimeNoMillis()
  }

  implicit val AgoraEntityFormat = jsonFormat10(AgoraEntity)

  implicit val AgoraEntityProjectionFormat = jsonFormat2(AgoraEntityProjection)

  implicit val AgoraErrorFormat = jsonFormat1(AgoraError)

  implicit val UserInfoResponseFormat = jsonFormat3(UserInfoResponse)
}
