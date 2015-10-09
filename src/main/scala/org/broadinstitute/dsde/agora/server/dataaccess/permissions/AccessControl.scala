package org.broadinstitute.dsde.agora.server.dataaccess.permissions

object AccessControl {

  def apply(accessObject: Tuple2[String, Int]): AccessControl = {
    val user = accessObject._1
    val permission = AgoraPermissions(accessObject._2)
    AccessControl(user, permission)
  }

  def fromParams(params: Map[String, String]): AccessControl = {
    val user = params.get("user")
    val roles = params.get("roles")

    if (user.isDefined && roles.isDefined)
      AccessControl(user.get, AgoraPermissions.fromParams(roles.get))
    else
      throw new IllegalArgumentException("Missing url params: user and/or roles.")
  }
}

case class AccessControl(user: String, roles: AgoraPermissions)