package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityAuthorizationException, NamespaceAuthorizationException, PermissionModificationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.dbio.DBIOAction

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class PermissionBusiness(permissionsDataSource: PermissionsDataSource)(implicit ec: ExecutionContext) {

  def listNamespacePermissions(entity: AgoraEntity, requester: String): Future[Seq[AccessControl]] = {
    permissionsDataSource.inTransaction { db =>
      authNamespaceRequester(db, entity, requester) {
        db.nsPerms.listNamespacePermissions(entity)
      }
    }
  }

  def insertNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authNamespaceRequester(db, entity, requester, accessObject) {
        db.nsPerms.insertNamespacePermission(entity, accessObject)
      }
    }
  }

  def batchNamespacePermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Future[Int] = {
    def authOne(db: DataAccess, accessObject: AccessControl) = {
      authNamespaceRequester(db, entity, requester, accessObject) {
        DBIOAction.successful()
      }
    }

    permissionsDataSource.inTransaction { db =>
      // Batch authorize, then batch insert.
      DBIOAction.sequence( accessObjectList.map(accessObject => authOne(db, accessObject)) ) andThen
      DBIOAction.sequence( accessObjectList.map(accessObject => db.nsPerms.insertNamespacePermission(entity, accessObject)) ) map { _.sum }
    }
  }

  def editNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authNamespaceRequester(db, entity, requester, accessObject) {
        db.nsPerms.editNamespacePermission(entity, accessObject)
      }
    }
  }

  def deleteNamespacePermission(entity: AgoraEntity, requester: String, userToRemove: String): Future[Int] = {
    Try(checkSameRequester(requester, userToRemove)) match {
      case Failure(regret) => Future.failed(regret)
      case Success(_) => {
        permissionsDataSource.inTransaction { db =>
          authNamespaceRequester(db, entity, requester) {
            db.nsPerms.deleteNamespacePermission(entity, userToRemove)
          }
        }
      }
    }
  }

  def listEntityPermissions(entity: AgoraEntity, requester: String): Future[Seq[AccessControl]] = {
    permissionsDataSource.inTransaction { db =>

    }
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.listEntityPermissions(entity)
  }

  def insertEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    authorizeEntityRequester(entity, requester, accessObject)
    AgoraEntityPermissionsClient.insertEntityPermission(entity, accessObject)
  }

  def batchEntityPermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Int = {
    // Batch authorize, then batch insert.
    accessObjectList.foreach(accessObject => authorizeEntityRequester(entity, requester, accessObject))
    accessObjectList.map(accessObject => AgoraEntityPermissionsClient.insertEntityPermission(entity, accessObject)).sum
  }

  def editEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Int = {
    authorizeEntityRequester(entity, requester, accessObject)
    AgoraEntityPermissionsClient.editEntityPermission(entity, accessObject)

  }

  def deleteEntityPermission(entity: AgoraEntity, requester: String, userToRemove: String): Int = {
    checkSameRequester(requester, userToRemove)
    authorizeEntityRequester(entity, requester)
    AgoraEntityPermissionsClient.deleteEntityPermission(entity, userToRemove)
  }


  /**
    * Utility method to both authorize the namespace requester and check that the requester is not modifying their own
    * permissions
    */
  private def authNamespaceRequester[T](db: DataAccess, entity: AgoraEntity, requester: String, accessObject: AccessControl)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withNamespaceACLs(db, entity, requester) { namespaceACLs =>
      checkSameRequesterAndPermissions(namespaceACLs.find(acl => acl.user.equals(requester)), accessObject)
      Try(authorizeNamespaceRequester(namespaceACLs, entity, requester)) match {
        case Success(_) => op
        case Failure(regret) => DBIOAction.failed(regret)
      }
    }
  }

  private def authNamespaceRequester[T](db: DataAccess, entity: AgoraEntity, requester: String)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withNamespaceACLs(db, entity, requester) { namespaceACLs =>
      Try( authorizeNamespaceRequester(namespaceACLs, entity, requester) ) match {
        case Success(_) => op
        case Failure(regret) => DBIOAction.failed(regret)
      }
    }

  }

  private def authorizeNamespaceRequester(acls: Seq[AccessControl], entity: AgoraEntity, requester: String): Unit = {
    if (!acls.exists(_.roles.canManage))
      throw NamespaceAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  /**
    * Utility method to both authorize the entity requester and check that the requester is not modifying their own
    * permissions
    */
  private def authEntityRequester[T](db: DataAccess, entity: AgoraEntity, requester: String, accessObject: AccessControl)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withEntityACLs(db, entity, requester) { entityACLs =>
      checkSameRequesterAndPermissions(entityACLs.find(acl => acl.user.equals(requester)), accessObject)
      Try(authorizeEntityRequester(entityACLs, entity, requester)) match {
        case Success(_) => op
        case Failure(regret) => DBIOAction.failed(regret)
      }
    }
  }

  private def authEntityRequester[T](db: DataAccess, entity: AgoraEntity, requester: String)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withEntityACLs(db, entity, requester) { entityACLs =>
      Try( authorizeEntityRequester(entityACLs, entity, requester) ) match {
        case Success(_) => op
        case Failure(regret) => DBIOAction.failed(regret)
      }
    }
  }

  private def authorizeEntityRequester(acls: Seq[AccessControl], entity: AgoraEntity, requester: String): Unit = {
    if (!acls.exists(_.roles.canManage))
      throw AgoraEntityAuthorizationException(AgoraPermissions(Manage), entity, requester)
  }

  private def checkSameRequester(requester: String, userToModify: String):Unit = {
    if (requester.equalsIgnoreCase(userToModify)) {
      throw PermissionModificationException()
    }
  }

  private def checkSameRequesterAndPermissions(currentACL: Option[AccessControl], newACL: AccessControl): Unit = {
    currentACL match {
      case Some(x) =>
        if (x.user.equalsIgnoreCase(newACL.user) && x.roles != newACL.roles) {
          throw PermissionModificationException()
        }
      case _ => Unit
    }
  }

  private def withNamespaceACLs[T](db: DataAccess, entity: AgoraEntity, requester: String)(op: Seq[AccessControl] => ReadWriteAction[T]): ReadWriteAction[T] = {
    db.nsPerms.listNamespacePermissions(entity) flatMap { perms =>
      val filteredPerms = perms.filter {
        perm => perm.user.equals(requester) || perm.user.equals("public")
      }
      op(filteredPerms)
    }
  }

  private def withEntityACLs[T](db: DataAccess, entity: AgoraEntity, requester: String)(op: Seq[AccessControl] => ReadWriteAction[T]): ReadWriteAction[T] = {
    db.aePerms.listEntityPermissions(entity) flatMap { perms =>
      val filteredPerms = perms.filter {
        perm => perm.user.equals(requester) || perm.user.equals("public")
      }
      op(filteredPerms)
    }
  }

}
