package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityAuthorizationException, NamespaceAuthorizationException, PermissionModificationException, ValidationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.dbio.DBIOAction

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalaz.{Failure => zFailure, Success => zSuccess}

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
    // validate entity
    AgoraEntity.validate(entity, allowEmptyIdentifiers=false) match {
      case zSuccess(_) => // noop
      case zFailure(errors) => throw new ValidationException(s"Entity [${entity.toShortString}] is not valid: Errors: $errors")
    }

    permissionsDataSource.inTransaction { db =>
      authEntityRequester(db, entity, requester) {
        db.aePerms.listEntityPermissions(entity)
      }
    }
  }

  def listEntityPermissions(entities: List[AgoraEntity], requester: String): Future[Seq[EntityAccessControl]] = {
    Future.sequence(entities map {entity =>
      listEntityPermissions(entity, requester) map { acls =>
        EntityAccessControl(entity, acls)
      } recover {
        // AgoraEntityAuthorizationException means we don't have permissions to read the entity's acls,
        // or the entity doesn't exist. For purposes of this method, call these non-fatal.
        // we don't recover from any other exceptions.
        case aeae:AgoraEntityAuthorizationException => EntityAccessControl(entity, Seq.empty[AccessControl], Some(aeae.getMessage))
      }
    })

  }

  def insertEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authEntityRequester(db, entity, requester, accessObject) {
        db.aePerms.insertEntityPermission(entity, accessObject)
      }
    }
  }

  def batchEntityPermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl]): Future[Int] = {
    def authOne(db: DataAccess, accessObject: AccessControl) = {
      authEntityRequester(db, entity, requester, accessObject) {
        DBIOAction.successful()
      }
    }
    permissionsDataSource.inTransaction { db =>
      // Batch authorize, then batch insert.
      DBIOAction.sequence( accessObjectList.map(accessObject => authOne(db, accessObject)) ) andThen
        DBIOAction.sequence( accessObjectList.map(accessObject => db.aePerms.insertEntityPermission(entity, accessObject)) ) map { _.sum }
    }
  }

  def editEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authEntityRequester(db, entity, requester, accessObject) {
        db.aePerms.editEntityPermission(entity, accessObject)
      }
    }
  }

  def deleteEntityPermission(entity: AgoraEntity, requester: String, userToRemove: String): Future[Int] = {
    Try(checkSameRequester(requester, userToRemove)) match {
      case Failure(regret) => Future.failed(regret)
      case Success(_) => {
        permissionsDataSource.inTransaction { db =>
          authEntityRequester(db, entity, requester) {
            db.aePerms.deleteEntityPermission(entity, userToRemove)
          }
        }
      }
    }
  }


  /**
    * Utility method to both authorize the namespace requester and check that the requester is not modifying their own
    * permissions
    */
  private def authNamespaceRequester[T](db: DataAccess, entity: AgoraEntity, requester: String, accessObject: AccessControl)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withNamespaceACLs(db, entity, requester) { namespaceACLs =>
      checkSameRequesterAndPermissions(namespaceACLs.find(acl => acl.user.equals(requester)), accessObject)
      authorizeNamespaceRequester(db, namespaceACLs, entity, requester) flatMap { _ => op }
    }
  }

  private def authNamespaceRequester[T](db: DataAccess, entity: AgoraEntity, requester: String)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withNamespaceACLs(db, entity, requester) { namespaceACLs =>
      authorizeNamespaceRequester(db, namespaceACLs, entity, requester) flatMap { _ => op }
    }

  }

  private def authorizeNamespaceRequester(db: DataAccess, acls: Seq[AccessControl], entity: AgoraEntity, requester: String): ReadWriteAction[Unit] = {
    //NOTE: The use of aePerms here seems wrong, but it's not!
    db.aePerms.addUserIfNotInDatabase(requester) map { _ =>
      if (!acls.exists(_.roles.canManage))
        throw NamespaceAuthorizationException(AgoraPermissions(Manage), entity, requester)
    }
  }

  /**
    * Utility method to both authorize the entity requester and check that the requester is not modifying their own
    * permissions
    */
  private def authEntityRequester[T](db: DataAccess, entity: AgoraEntity, requester: String, accessObject: AccessControl)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withEntityACLs(db, entity, requester) { entityACLs =>
      checkSameRequesterAndPermissions(entityACLs.find(acl => acl.user.equals(requester)), accessObject)
      authorizeEntityRequester(db, entityACLs, entity, requester) flatMap { _ => op }
    }
  }

  private def authEntityRequester[T](db: DataAccess, entity: AgoraEntity, requester: String)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    withEntityACLs(db, entity, requester) { entityACLs =>
      authorizeEntityRequester(db, entityACLs, entity, requester) flatMap { _ => op }
    }
  }

  private def authorizeEntityRequester(db: DataAccess, acls: Seq[AccessControl], entity: AgoraEntity, requester: String): ReadWriteAction[Unit] = {
    db.aePerms.addUserIfNotInDatabase(requester) map { _ =>
      if (!acls.exists(_.roles.canManage))
        throw AgoraEntityAuthorizationException(AgoraPermissions(Manage), entity, requester)
    }
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
