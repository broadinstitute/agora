package org.broadinstitute.dsde.agora.server.model

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


object AgoraEntityBsonSupport {

  def read(obj: DBObject): AgoraEntity =
    AgoraEntity(
      namespace       = stringOrNone(obj, "namespace"),
      name            = stringOrNone(obj, "name"),
      snapshotId      = intOrNone(obj, "snapshotId"),
      snapshotComment = stringOrNone(obj, "snapshotComment"),
      synopsis        = stringOrNone(obj, "synopsis"),
      documentation   = stringOrNone(obj, "documentation"),
      owner           = stringOrNone(obj, "owner"), // Not in DB but checked by tests. Just rolling with this for now.
      createDate      = dateOrNone(obj, "createDate"),
      payload         = stringOrNone(obj, "payload"),
      payloadObject   = None, // Not in DB
      url             = None, // Not in DB
      entityType      = entityType(obj),
      id              = Option(new ObjectId(obj.get("_id").toString)),
      methodId        = idOrNone(obj, "methodId"), // Configs only
      method          = None, // Not in DB
      managers        = Seq.empty, // Not in DB
      public          = None, // Not in DB
    )

  private def idOrNone(obj: DBObject, key: String): Option[ObjectId] = {
    if (obj.containsField(key)) Option(new ObjectId(obj.get(key).toString)) else None
  }

  private def stringOrNone(obj: DBObject, key: String): Option[String] = {
    if (obj.containsField(key)) Option(obj.get(key).asInstanceOf[String]) else None
  }

  private def intOrNone(obj: DBObject, key: String): Option[Int] = {
    if (obj.containsField(key)) Option(obj.get(key).asInstanceOf[Int]) else None
  }

  private def dateOrNone(obj: DBObject, key: String): Option[DateTime] = {
    if (obj.containsField(key)) Option(DateTime.parse(obj.get(key).asInstanceOf[String], ISODateTimeFormat.dateTimeNoMillis())) else None
  }

  private def entityType(obj: DBObject): Option[AgoraEntityType.EntityType] = {
    if (obj.containsField("entityType")) {
      val entityString = Option(obj.get("entityType").asInstanceOf[String])
      entityString.map(AgoraEntityType.withName)
    } else {
      None
    }
  }

}
