package org.broadinstitute.dsde.agora.server.model

import com.mongodb.DBObject
import org.joda.time.DateTime


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
      createDate      = dateOrNone(obj, "createDate"),
      payload         = stringOrNone(obj, "payload"),
      payloadObject   = None, // Not in DB
      url             = None, // Not in DB
      entityType      = Option(AgoraEntityType.Workflow), // TODO happens to be true in Adam's DB at the moment...
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

  private def dateOrNone(obj: DBObject, key: String): Option[DateTime] = {
    Option(DateTime.now()) // TODO needs proper parsing...
  }

//  private def entityTypeOrNone(obj: DBObject, key: String): Option[AgoraEntityType.EntityType] = {
//    import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
//    import org.broadinstitute.dsde.agora.server.model.AgoraEntityType._
//    if (obj.containsField(key)) {
//      val entityString = Option(obj.get(key).asInstanceOf[String])
//
//    } else
//      None
//  }

}
