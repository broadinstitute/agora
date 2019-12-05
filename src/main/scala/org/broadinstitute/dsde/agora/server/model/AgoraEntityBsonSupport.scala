package org.broadinstitute.dsde.agora.server.model

import com.mongodb.DBObject
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


object AgoraEntityBsonSupport {

  def read(obj: DBObject): AgoraEntity =
    AgoraEntity(
      namespace = getOrNone(obj, "namespace"),
      name = getOrNone(obj, "name"),
      snapshotId = getOrNone(obj, "snapshotId"),
      snapshotComment = getOrNone(obj, "snapshotComment"),
      synopsis = getOrNone(obj, "synopsis"),
      documentation = getOrNone(obj, "documentation"),
      owner = getOrNone(obj, "owner"), // Not in DB but checked by tests. Just rolling with this for now.
      createDate = dateOrNone(obj, "createDate"),
      payload = getOrNone(obj, "payload"),
      payloadObject = None, // Not in DB
      url = None, // Not in DB
      entityType = entityType(obj),
      id = getOrNone(obj, "_id"),
      methodId = getOrNone(obj, "methodId"), // Configs only
      method = None, // Not in DB
      managers = Seq.empty, // Not in DB
      public = None, // Not in DB
    )

  private def getOrNone[A](obj: DBObject, key: String): Option[A] = {
    Option(obj.get(key)).map(_.asInstanceOf[A])
  }

  private def dateOrNone(obj: DBObject, key: String): Option[DateTime] = {
    val dateString = getOrNone[String](obj, key)
    dateString.map(DateTime.parse(_, ISODateTimeFormat.dateTimeNoMillis()))
  }

  private def entityType(obj: DBObject): Option[AgoraEntityType.EntityType] = {
    val entityString = getOrNone[String](obj, "entityType")
    entityString.map(AgoraEntityType.withName)
  }

}
