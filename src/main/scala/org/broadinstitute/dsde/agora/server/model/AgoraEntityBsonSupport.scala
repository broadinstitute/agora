package org.broadinstitute.dsde.agora.server.model

import java.time.OffsetDateTime

import org.mongodb.scala.Document
import org.mongodb.scala.bson._

object AgoraEntityBsonSupport {

  def read(obj: Document): AgoraEntity =
    AgoraEntity(
      namespace = stringOrNone(obj, "namespace"),
      name = stringOrNone(obj, "name"),
      snapshotId = intOrNone(obj, "snapshotId"),
      snapshotComment = stringOrNone(obj, "snapshotComment"),
      synopsis = stringOrNone(obj, "synopsis"),
      documentation = stringOrNone(obj, "documentation"),
      owner = stringOrNone(obj, "owner"), // Not in DB but checked by tests. Just rolling with this for now.
      createDate = dateOrNone(obj, "createDate"),
      payload = stringOrNone(obj, "payload"),
      payloadObject = None, // Not in DB
      url = None, // Not in DB
      entityType = entityType(obj),
      id = objectIdOrNone(obj, "_id"),
      methodId = objectIdOrNone(obj, "methodId"), // Configs only
      method = None, // Not in DB
      managers = Seq.empty, // Not in DB
      public = None, // Not in DB
    )

  private def getOrNone[A <: BsonValue](obj: Document, key: String): Option[A] = {
    obj.get(key).map(_.asInstanceOf[A])
  }

  private def stringOrNone(obj: Document, key: String): Option[String] = {
    getOrNone[BsonString](obj, key).map(_.getValue)
  }

  //noinspection SameParameterValue
  private def intOrNone(obj: Document, key: String): Option[Int] = {
    getOrNone[BsonInt32](obj, key).map(_.getValue)
  }

  private def objectIdOrNone(obj: Document, key: String): Option[ObjectId] = {
    getOrNone[BsonObjectId](obj, key).map(_.getValue)
  }

  //noinspection SameParameterValue
  private def dateOrNone(obj: Document, key: String): Option[OffsetDateTime] = {
    stringOrNone(obj, key).map(OffsetDateTime.parse)
  }

  private def entityType(obj: Document): Option[AgoraEntityType.EntityType] = {
    val entityString = stringOrNone(obj, "entityType")
    entityString.map(AgoraEntityType.withName)
  }

}
