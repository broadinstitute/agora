package org.broadinstitute.dsde.agora.server.model

import com.mongodb.DBObject


object AgoraEntityBsonSupport {

  def read(obj: DBObject): AgoraEntity =
    AgoraEntity(
      namespace       = stringOrNone(obj, "namespace"),
      name            = stringOrNone(obj, "name"),
      snapshotId      = intOrNone(obj, "snapshotId"),
      snapshotComment = stringOrNone(obj, "snapshotComment"),
      synopsis        = stringOrNone(obj, "synopsis"),
      documentation   = stringOrNone(obj, "documentation"),
      owner           = None, // Not in DB
      createDate      = None, // AEN TODO
      payload         = stringOrNone(obj, "payload"),
      payloadObject   = None, // Not in DB
      url             = None, // Not in DB
      entityType      = None, // AEN TODO
      id              = None, // In DB, but I don't think we ever use this
      methodId        = None, // Not in DB
      method          = None, // Not in DB
      managers        = Seq.empty, // Not in DB
      public          = None, // Not in DB
    )

  private def stringOrNone(obj: DBObject, key: String): Option[String] = {
    if (obj.containsField(key)) Option(obj.get(key).asInstanceOf[String]) else None
  }

  private def intOrNone(obj: DBObject, key: String): Option[Int] = {
    if (obj.containsField(key)) Option(obj.get(key).asInstanceOf[Int]) else None
  }

}
