
package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.webservice.util.AgoraOpenAMClient.UserInfoResponse
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AgoraPermissions, AccessControl}
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

  implicit object AgoraEntityTypeFormat extends RootJsonFormat[AgoraEntityType.EntityType] {
    override def write(obj: AgoraEntityType.EntityType): JsValue = JsString(obj.toString)

    override def read(value: JsValue): AgoraEntityType.EntityType = value match {
      case JsString(name) => AgoraEntityType.withName(name)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  implicit object UserInfoResponseFormat extends RootJsonFormat[UserInfoResponse] {
    override def write(userInfo: UserInfoResponse) = {
      JsObject("username" -> JsString(userInfo.username), "cn" -> userInfo.cn.toJson, "mail" -> userInfo.mail.toJson)
    }

    override def read(json: JsValue): UserInfoResponse = json match {
      case x: JsObject =>
        val username = x.fields("username").convertTo[String]
        val cn = x.fields("cn").convertTo[Seq[String]]
        val mailJson = x.fields.get("mail")
        val mail = mailJson match {
          case Some(emailJson) => emailJson.convertTo[Seq[String]]
          case None => Seq.empty[String]
        }
        UserInfoResponse(username, cn, mail)
      case _ => throw new DeserializationException("only string supported")
    }
  }

  private val parserISO: DateTimeFormatter = {
    ISODateTimeFormat.dateTimeNoMillis()
  }

  implicit object AgoraPermissionsFormat extends RootJsonFormat[AgoraPermissions] {
    override def write(obj: AgoraPermissions): JsArray =
      JsArray(obj.toListOfStrings.map(JsString.apply))

    override def read(json: JsValue): AgoraPermissions = json match {
      case array: JsArray =>
        val listOfStrings = array.convertTo[Seq[String]]
        AgoraPermissions(listOfStrings)
      case _ => throw new DeserializationException("unsupported AgoraPermission")
    }

  }

  implicit val AgoraEntityFormat = jsonFormat10(AgoraEntity.apply)

  implicit val AgoraEntityProjectionFormat = jsonFormat2(AgoraEntityProjection.apply)

  implicit val AgoraErrorFormat = jsonFormat1(AgoraError)

  implicit val AccessControlFormat = jsonFormat2(AccessControl.apply)


}
