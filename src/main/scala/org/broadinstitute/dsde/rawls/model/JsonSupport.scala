package org.broadinstitute.dsde.rawls.model

import spray.json._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}

class JsonSupport {
  import spray.json.DefaultJsonProtocol._

  /* David An: this is not exactly the same AttributeStringFormat as in rawls-model;
      I've simplified it to keep the copy-paste to a minimum. It should act exactly the same.
   */
  implicit object AttributeStringFormat extends RootJsonFormat[AttributeString] {
    override def write(obj: AttributeString): JsValue = JsString(obj.value)
    override def read(json: JsValue): AttributeString = json match {
      case JsString(s) => AttributeString(s)
      case _ => throw new DeserializationException("unexpected json type")
    }
  }

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    private val parserISO : DateTimeFormatter = {
      ISODateTimeFormat.dateTime
    }

    override def write(obj: DateTime) = {
      JsString(parserISO.print(obj))
    }

    override def read(json: JsValue): DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw new DeserializationException("only string supported")
    }
  }
}
