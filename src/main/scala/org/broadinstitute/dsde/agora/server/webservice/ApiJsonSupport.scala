package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.joda.time.{DateTime, DateTimeZone}
import spray.httpx.SprayJsonSupport
import spray.json._

object ApiJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val AgoraEntityFormat = jsonFormat8(AgoraEntity)

  implicit val DateFormat = new RootJsonFormat[Option[java.sql.Date]] {
    lazy val format = new java.text.SimpleDateFormat()
    def read(json: JsValue): java.sql.Date = new java.sql.Date(json.##)
    def write(date: Option[java.sql.Date]) = JsString(new DateTime(date.get.getTime, DateTimeZone.UTC).withZone(DateTimeZone.forID("UTC")).getMillis.toString)
  }
}
