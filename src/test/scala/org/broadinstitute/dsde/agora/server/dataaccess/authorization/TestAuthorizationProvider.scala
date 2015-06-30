
package org.broadinstitute.dsde.agora.server.dataaccess.authorization

import org.broadinstitute.dsde.agora.server.AgoraTestData
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AuthorizedAgoraEntity, AuthorizationProvider, AgoraPermissions}
import scala.collection.mutable.HashMap
import AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

//Test authorization implementation. Add authorizations directly to the hash map for lookup.
// If no local permissions exist, full authorization is granted.
object TestAuthorizationProvider extends AuthorizationProvider with AgoraTestData{

  // local permission storage.
  val entityPermissions = new HashMap[String, AgoraPermissions]()
  val namespacePermissions = new HashMap[String, AgoraPermissions]()

  def addEntityPermission(hash: String, permission: AgoraPermissions) = entityPermissions.put(hash, permission)
  def addNamespacePermission(hash: String, permission: AgoraPermissions) = namespacePermissions.put(hash, permission)

  override def createEntityAuthorizations(entity: AgoraEntity, username: String): Unit = {
    addNamespacePermission(hash(entity, username), AgoraPermissions(All))
    addEntityPermission(hash(entity, username), AgoraPermissions(All))
  }

  override def namespaceAuthorization(entity: AgoraEntity, username: String): AgoraPermissions = {
    namespacePermissions.getOrElse(hash(entity, username), AgoraPermissions(All))
  }

  override def entityAuthorization(entity: AgoraEntity, username: String): AgoraPermissions = {
    entityPermissions.getOrElse(hash(entity, username), AgoraPermissions(All))
  }

  //helpers
  def uniqueString(e: AgoraEntity, user: String): String = e.namespace + ":" + e.name + ":" + e.snapshotId + ":" + user
  def md5Hash(text: String) : String = java.security.MessageDigest.getInstance("MD5").digest(text.getBytes()).map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _}
  def hash(e: AgoraEntity, user: String) = md5Hash(uniqueString(e, user))
}
