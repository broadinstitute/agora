package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._

/**
 * Stores information about public aliases along with owners of aliases.
 *
 * @param publicAliasesSet    Set of public aliases
 * @param ownersAndAliasesMap Map of aliases, and a Seq of owners with manage permissions
 */
case class OwnersAndAliases(publicAliasesSet: Set[String], ownersAndAliasesMap: Map[String, Seq[String]])

object OwnersAndAliases {
  def from(aliasesOwnersPermissions: Seq[(String, String, Int)]): OwnersAndAliases = {
    val publicAliasesSet: Set[String] = aliasesOwnersPermissions.collect({
      case (alias, owner, permissions)
        if owner == AccessControl.publicUser && permissions > AgoraPermissions.Nothing =>
        alias
    }).toSet

    val ownersAndAliases = aliasesOwnersPermissions.collect({
      case (alias, owner, permissions) if permissions.hasPermission(AgoraPermissions.Manage) => (alias, owner)
    })

    // In Scala 2.13 this can be replaced with a .groupMap()
    val ownersAndAliasesMap: Map[String, Seq[String]] = ownersAndAliases.groupBy(_._1).mapValues(_.map(_._2))

    OwnersAndAliases(publicAliasesSet, ownersAndAliasesMap)
  }
}
