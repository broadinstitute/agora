package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityAuthorizationException, NamespaceAuthorizationException, PermissionModificationException, ValidationException}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.dbio.DBIOAction

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import cats.data.Validated._

class PermissionBusiness(permissionsDataSource: PermissionsDataSource) {

  def listNamespacePermissions(entity: AgoraEntity, requester: String)
                              (implicit executionContext: ExecutionContext): Future[Seq[AccessControl]] = {
    permissionsDataSource.inTransaction { db =>
      authNamespaceRequester(db, entity, requester) {
        db.nsPerms.listNamespacePermissions(entity)
      }
    }
  }

  def insertNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl)
                               (implicit executionContext: ExecutionContext): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authNamespaceRequester(db, entity, requester, accessObject) {
        db.nsPerms.insertNamespacePermission(entity, accessObject)
      }
    }
  }

  def batchNamespacePermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl])
                              (implicit executionContext: ExecutionContext): Future[Int] = {
    def authOne(db: DataAccess, accessObject: AccessControl) = {
      authNamespaceRequester(db, entity, requester, accessObject) {
        DBIOAction.successful(())
      }
    }

    permissionsDataSource.inTransaction { db =>
      // Batch authorize, then batch insert.
      DBIOAction.sequence( accessObjectList.map(accessObject => authOne(db, accessObject)) ) andThen
      DBIOAction.sequence( accessObjectList.map(accessObject => db.nsPerms.insertNamespacePermission(entity, accessObject)) ) map { _.sum }
    }
  }

  def editNamespacePermission(entity: AgoraEntity, requester: String, accessObject: AccessControl)
                             (implicit executionContext: ExecutionContext): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authNamespaceRequester(db, entity, requester, accessObject) {
        db.nsPerms.editNamespacePermission(entity, accessObject)
      }
    }
  }

  def deleteNamespacePermission(entity: AgoraEntity, requester: String, userToRemove: String)
                               (implicit executionContext: ExecutionContext): Future[Int] = {
    Try(checkSameRequester(requester, userToRemove)) match {
      case Failure(regret) => Future.failed(regret)
      case Success(_) =>
        permissionsDataSource.inTransaction { db =>
          authNamespaceRequester(db, entity, requester) {
            db.nsPerms.deleteNamespacePermission(entity, userToRemove)
          }
        }
    }
  }

  def listEntityPermissions(entity: AgoraEntity, requester: String)
                           (implicit executionContext: ExecutionContext): Future[Seq[AccessControl]] = {
    // validate entity
    AgoraEntity.validate(entity, allowEmptyIdentifiers=false) match {
      case Valid(_) => // noop
      case Invalid(errors) =>
        throw ValidationException(s"Entity [${entity.toShortString}] is not valid: Errors: $errors")
    }

    permissionsDataSource.inTransaction { db =>
      authEntityRequester(db, entity, requester) {
        db.aePerms.listEntityPermissions(entity)
      }
    }
  }

  def listEntityPermissions(entities: List[AgoraEntity], requester: String)
                           (implicit executionContext: ExecutionContext): Future[Seq[EntityAccessControl]] = {
    Future.sequence(entities map {entity =>
      permissionsDataSource.inTransaction { db =>
        for {
          owners <- db.aePerms.listOwners(entity)
          publicAccess <- db.aePerms.getPermission(entity, AccessControl.publicUser)
        } yield {
          (owners, publicAccess)
        }
      } flatMap { case (owners:Seq[String], publicAccess:AgoraPermissions) =>
        val annotatedEntity = entity.copy(managers = owners, public = Some(publicAccess.canRead))
        listEntityPermissions(entity, requester) map { acls =>
          EntityAccessControl(annotatedEntity, acls)
        } recover {
          // AgoraEntityAuthorizationException means we don't have permissions to read the entity's acls,
          // or the entity doesn't exist. For purposes of this method, call these non-fatal.
          // we don't recover from any other exceptions. Additionally, for unauthorized access return entity information
          // that was already part of request. Don't send additional information. This meets previous response expectations
          // and fixes the issue mentioned in https://broadworkbench.atlassian.net/browse/WX-1764.
          case aeae: AgoraEntityAuthorizationException => EntityAccessControl(entity, Seq.empty[AccessControl], Some(aeae.getMessage))
        }
      }
    })
  }

  def upsertEntityPermissions(aclPairs: List[EntityAccessControl], requester: String)
                             (implicit executionContext: ExecutionContext): Future[Seq[EntityAccessControl]] = {

    if (aclPairs.isEmpty)
      Future.successful(Seq.empty[EntityAccessControl])
    else {

      val groupedAccessControl:Map[AgoraEntity, List[AccessControl]] = aclPairs.groupBy(_.entity).map {
        case (entity, listEntityAccessControl) => (entity, listEntityAccessControl flatMap(_.acls))
      }

      // run these sequentially to avoid DB deadlocks
      val batchFutures = groupedAccessControl map {
        case (entity, listAccessControl) => () =>
          batchEntityPermission(entity, requester, listAccessControl) map { _ =>
            EntityAccessControl(entity, listAccessControl, None)
          } recover {
            // For unauthorized access return entity information that was already in the request and empty ACLs values.
            // This meets previous response expectations, keeps response schema similar to POST API and fixes the
            // issue mentioned in https://broadworkbench.atlassian.net/browse/WX-1764.
            case aeae: AgoraEntityAuthorizationException =>
              EntityAccessControl(entity, Seq.empty[AccessControl], Some(aeae.getMessage))
            case e:Exception =>
              EntityAccessControl(entity, listAccessControl, Some(e.getMessage))
          }
      }

      def sequentially(futures: Iterable[() => Future[EntityAccessControl]], acc: Seq[EntityAccessControl]): Future[Seq[EntityAccessControl]] = {
        futures match {
          case x if x.isEmpty => Future.successful(acc)
          case _ =>
            futures.head() flatMap { eac =>
              sequentially(futures.tail, acc :+ eac)
            }
        }
      }

      sequentially(batchFutures, Seq.empty[EntityAccessControl])
    }
  }

  def insertEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl)
                            (implicit executionContext: ExecutionContext): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authEntityRequester(db, entity, requester, accessObject) {
        db.aePerms.insertEntityPermission(entity, accessObject)
      }
    }
  }

  def batchEntityPermission(entity: AgoraEntity, requester: String, accessObjectList: List[AccessControl])
                           (implicit executionContext: ExecutionContext): Future[Int] = {
    def authOne(db: DataAccess, accessObject: AccessControl) = {
      authEntityRequester(db, entity, requester, accessObject) {
        DBIOAction.successful(())
      }
    }
    permissionsDataSource.inTransaction { db =>
      // Batch authorize, then batch insert.
      DBIOAction.sequence( accessObjectList.map(accessObject => authOne(db, accessObject)) ) andThen
        DBIOAction.sequence( accessObjectList.map(accessObject => db.aePerms.insertEntityPermission(entity, accessObject)) ) map { _.sum }
    }
  }

  def editEntityPermission(entity: AgoraEntity, requester: String, accessObject: AccessControl)
                          (implicit executionContext: ExecutionContext): Future[Int] = {
    permissionsDataSource.inTransaction { db =>
      authEntityRequester(db, entity, requester, accessObject) {
        db.aePerms.editEntityPermission(entity, accessObject)
      }
    }
  }

  def deleteEntityPermission(entity: AgoraEntity, requester: String, userToRemove: String)
                            (implicit executionContext: ExecutionContext): Future[Int] = {
    Try(checkSameRequester(requester, userToRemove)) match {
      case Failure(regret) => Future.failed(regret)
      case Success(_) =>
        permissionsDataSource.inTransaction { db =>
          authEntityRequester(db, entity, requester) {
            db.aePerms.deleteEntityPermission(entity, userToRemove)
          }
        }
    }
  }


  /**
    * Utility method to both authorize the namespace requester and check that the requester is not modifying their own
    * permissions
    */
  private def authNamespaceRequester[T](db: DataAccess,
                                        entity: AgoraEntity,
                                        requester: String,
                                        accessObject: AccessControl)
                                       (op: => ReadWriteAction[T])
                                       (implicit executionContext: ExecutionContext): ReadWriteAction[T] = {
    withNamespaceACLs(db, entity, requester) { namespaceACLs =>
      checkSameRequesterAndPermissions(namespaceACLs.find(acl => acl.user.equals(requester)), accessObject)
      authorizeNamespaceRequester(db, namespaceACLs, entity, requester) flatMap { _ => op }
    }
  }

  private def authNamespaceRequester[T](db: DataAccess, entity: AgoraEntity, requester: String)
                                       (op: => ReadWriteAction[T])
                                       (implicit executionContext: ExecutionContext): ReadWriteAction[T] = {
    withNamespaceACLs(db, entity, requester) { namespaceACLs =>
      authorizeNamespaceRequester(db, namespaceACLs, entity, requester) flatMap { _ => op }
    }

  }

  private def authorizeNamespaceRequester(db: DataAccess,
                                          acls: Seq[AccessControl],
                                          entity: AgoraEntity,
                                          requester: String)
                                         (implicit executionContext: ExecutionContext): ReadWriteAction[Unit] = {
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
  private def authEntityRequester[T](db: DataAccess,
                                     entity: AgoraEntity,
                                     requester: String,
                                     accessObject: AccessControl)
                                    (op: => ReadWriteAction[T])
                                    (implicit executionContext: ExecutionContext): ReadWriteAction[T] = {
    withEntityACLs(db, entity, requester) { entityACLs =>
      checkSameRequesterAndPermissions(entityACLs.find(acl => acl.user.equals(requester)), accessObject)
      authorizeEntityRequester(db, entityACLs, entity, requester) flatMap { _ => op }
    }
  }

  private def authEntityRequester[T](db: DataAccess, entity: AgoraEntity, requester: String)
                                    (op: => ReadWriteAction[T])
                                    (implicit executionContext: ExecutionContext): ReadWriteAction[T] = {
    withEntityACLs(db, entity, requester) { entityACLs =>
      authorizeEntityRequester(db, entityACLs, entity, requester) flatMap { _ => op }
    }
  }

  private def authorizeEntityRequester(db: DataAccess, acls: Seq[AccessControl], entity: AgoraEntity, requester: String)
                                      (implicit executionContext: ExecutionContext): ReadWriteAction[Unit] = {
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
      case _ => ()
    }
  }

  private def withNamespaceACLs[T](db: DataAccess, entity: AgoraEntity, requester: String)
                                  (op: Seq[AccessControl] => ReadWriteAction[T])
                                  (implicit executionContext: ExecutionContext): ReadWriteAction[T] = {
    db.nsPerms.listNamespacePermissions(entity) flatMap filterRequesterOrPublic(requester, op)
  }

  private def withEntityACLs[T](db: DataAccess, entity: AgoraEntity, requester: String)
                               (op: Seq[AccessControl] => ReadWriteAction[T])
                               (implicit executionContext: ExecutionContext): ReadWriteAction[T] = {
    db.aePerms.listEntityPermissions(entity) flatMap filterRequesterOrPublic(requester, op)
  }

  private def filterRequesterOrPublic[T](requester: String, op: Seq[AccessControl] => ReadWriteAction[T])
                                        (perms: Seq[AccessControl]): ReadWriteAction[T] = {
    val filteredPerms = perms.filter {
      perm => perm.user.equals(requester) || perm.user.equals(AccessControl.publicUser)
    }
    op(filteredPerms)
  }
}
