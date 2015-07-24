package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Actor
import com.google.api.services.storage.model.{ObjectAccessControl, BucketAccessControl}
import org.broadinstitute.dsde.agora.server.busines.AclBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import spray.routing.RequestContext

class AclHandler(authorizationProvider: AuthorizationProvider) extends Actor {
  implicit val system = context.system
  val aclBusiness = new AclBusiness(authorizationProvider)

  def receive = {
    case ListNamespaceAcls(_context: RequestContext, entity: AgoraEntity, username: String) =>
      val acls = aclBusiness.listNamespaceAcls(_context, entity, username)
      context.parent ! RequestComplete(acls)
      context.stop(self)

    case InsertNamespaceAcl(_context: RequestContext, entity: AgoraEntity, username: String, acl: BucketAccessControl) =>
      val acls = aclBusiness.insertNamespaceAcl(_context, entity, username, acl)
      context.parent ! RequestComplete(acls)
      context.stop(self)

    case DeleteNamespaceAcl(_context: RequestContext, entity: AgoraEntity, username: String, acl: BucketAccessControl) =>
      val acls = aclBusiness.deleteNamespaceAcl(_context, entity, username, acl)
      context.parent ! RequestComplete(acls)
      context.stop(self)

    case ListEntityAcls(_context: RequestContext, entity: AgoraEntity, username: String) =>
      val acls = aclBusiness.listEntityAcls(_context, entity, username)
      context.parent ! RequestComplete(acls)
      context.stop(self)

    case InsertEntityAcl(_context: RequestContext, entity: AgoraEntity, username: String, acl: ObjectAccessControl) =>
      val acls = aclBusiness.insertEntityAcl(_context, entity, username, acl)
      context.parent ! RequestComplete(acls)
      context.stop(self)

    case DeleteEntityAcl(_context: RequestContext, entity: AgoraEntity, username: String, acl: ObjectAccessControl) =>
      val acls = aclBusiness.deleteEntityAcl(_context, entity, username, acl)
      context.parent ! RequestComplete(acls)
      context.stop(self)
  }

}
