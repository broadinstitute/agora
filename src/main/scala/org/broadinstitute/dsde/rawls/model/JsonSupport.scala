package org.broadinstitute.dsde.rawls.model

import spray.json._
import java.time.{OffsetDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter

class JsonSupport {

  /* David An: this is not exactly the same AttributeStringFormat as in rawls-model;
      I've simplified it to keep the copy-paste to a minimum. It should act exactly the same.
   */
  implicit object AttributeStringFormat extends RootJsonFormat[AttributeString] {
    override def write(obj: AttributeString): JsValue = JsString(obj.value)
    override def read(json: JsValue): AttributeString = json match {
      case JsString(s) => AttributeString(s)
      case _ => throw DeserializationException("unexpected json type")
    }
  }

  implicit object DateJsonFormat extends RootJsonFormat[OffsetDateTime] {
    /**
     * Instead of "one of" the valid ISO-8601 formats, standardize on this one:
     * https://github.com/openjdk/jdk/blob/jdk8-b120/jdk/src/share/classes/java/time/OffsetDateTime.java#L1886
     */
    private val Iso8601MillisecondsFormat = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX")

    private def utcWithMillis(offsetDateTime: OffsetDateTime): String = {
      offsetDateTime.atZoneSameInstant(ZoneOffset.UTC).format(Iso8601MillisecondsFormat)
    }

    override def write(offsetDateTime: OffsetDateTime): JsString = {
      JsString(utcWithMillis(offsetDateTime))
    }

    override def read(json: JsValue): OffsetDateTime = json match {
      case JsString(string) => OffsetDateTime.parse(string)
      case _ => throw DeserializationException("only string supported")
    }
  }
}
