
package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.webservice.util.AgoraOpenAMClient.UserInfoResponse
import org.broadinstitute.dsde.agora.server.webservice.validation.AgoraValidation
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import spray.json._

import scala.language.implicitConversions

object AgoraApiJsonSupport extends DefaultJsonProtocol {

  implicit def stringToDateTime(str: String): DateTime = parserISO.parseDateTime(str)

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    override def write(obj: DateTime) = {
      JsString(parserISO.print(obj))
    }

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  private val parserISO: DateTimeFormatter = {
    ISODateTimeFormat.dateTimeNoMillis()
  }

  implicit val AgoraEntityFormat = jsonFormat9(AgoraEntity)

  implicit val AgoraEntityProjectionFormat = jsonFormat2(AgoraEntityProjection)

  implicit val AgoraErrorFormat = jsonFormat1(AgoraError)

  implicit object AgoraValidationFormat extends RootJsonFormat[AgoraValidation] {
    override def write(validation: AgoraValidation) = {
      Map("error" -> validation.messages).toJson
    }

    override def read(json: JsValue): AgoraValidation = json match {
      case x: JsObject => {
        val messages = x.fields("error").convertTo[Seq[String]]
        AgoraValidation(messages)
      }
      case _ => throw new DeserializationException("only string supported")
    }
  }

  implicit val UserInfoResponseFormat = jsonFormat3(UserInfoResponse)
}
