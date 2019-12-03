package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.{MetricsClient, ReadAction, ReadWriteAction, WriteAction}
import org.broadinstitute.dsde.agora.server.exceptions.PermissionNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import slick.jdbc.JdbcProfile
import spray.json.{JsNumber, JsObject, JsString}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

abstract class PermissionsClient(profile: JdbcProfile) extends LazyLogging {
  import profile.api._

  lazy val metricsClient = new MetricsClient()

  def alias(entity: AgoraEntity): String

  def withPermissionNotFoundException[T](errorString: String = "Could not get permission")
                                        (op: => ReadWriteAction[T])
                                        (implicit executionContext: ExecutionContext): ReadWriteAction[T] = {
    op.asTry flatMap {
      case Success(permission) => DBIO.successful(permission)
      case Failure(ex) => DBIO.failed(PermissionNotFoundException(errorString, ex))
    }
  }

  // Users
  def addUserIfNotInDatabase(userEmail: String)(implicit executionContext: ExecutionContext): WriteAction[Int] = {
    // Attempts to add user to UserTable and ignores errors if user already exists
    (users += UserDao(userEmail)).asTry flatMap {
      case Success(count) => DBIO.successful(count)
      case Failure(_) => DBIO.successful(0)
    }
  }

  def isAdmin(userEmail: String)(implicit executionContext: ExecutionContext): ReadAction[Boolean] = {
    users.findByEmail(userEmail).result map { users =>
      users.head.isAdmin
    }
  }

  def listAdmins()(implicit executionContext: ExecutionContext): ReadWriteAction[Seq[String]] = {
    withPermissionNotFoundException() {
      val adminsQuery = for {
        user <- users if user.is_admin === true
      } yield user.email

      adminsQuery.result
    }
  }

  def updateAdmin(userEmail: String, adminStatus: Boolean)
                 (implicit executionContext: ExecutionContext): ReadWriteAction[Int] = {
    withPermissionNotFoundException(s"Could not make user $userEmail admin") {
      for {
        _ <- addUserIfNotInDatabase(userEmail)
        rowsUpdated <- users
          .filter(_.email === userEmail)
          .map(_.is_admin)
          .update(adminStatus)
      } yield {
        if( rowsUpdated == 0 ) {
          throw new Exception("No rows were edited.")
        } else {
          rowsUpdated
        }
      }
    }
  }

  // Entities
  def addEntity(entity: AgoraEntity): WriteAction[Int] = {
    entities += EntityDao(alias(entity))
  }

  def doesEntityExists(agoraEntity: AgoraEntity)(implicit executionContext: ExecutionContext): ReadAction[Boolean] = {
    entities.findByAlias(alias(agoraEntity)).result map { entity =>
      entity.nonEmpty
    }
  }

  private def permissionQuery(agoraEntity: AgoraEntity, userEmail: String)
                             (implicit executionContext: ExecutionContext): ReadAction[Option[PermissionDao]] = {
    // Construct query to get permissions
    for {
      user <- users
        .filter(_.email === userEmail)
        .result
        .head

      entity <- entities
        .filter(_.alias === alias(agoraEntity))
        .result
        .head

      permission <- permissions
        .filter(p => p.entityID === entity.id && p.userID === user.id)
        .result
        .headOption
    } yield permission
  }


  private def lookupPermissions(agoraEntity: AgoraEntity, userEmail: String)
                               (implicit executionContext: ExecutionContext): ReadWriteAction[AgoraPermissions] = {
    for {
      _ <- addUserIfNotInDatabase(userEmail)
      permissionResultOpt <- permissionQuery(agoraEntity, userEmail)
    } yield {
      if (permissionResultOpt.isEmpty)
        AgoraPermissions(Nothing)
      else
        AgoraPermissions(permissionResultOpt.get.roles)
    }
  }

  // Permissions
  def getPermission(agoraEntity: AgoraEntity, userEmail: String)
                   (implicit executionContext: ExecutionContext): ReadWriteAction[AgoraPermissions] = {
    withPermissionNotFoundException() {
      doesEntityExists(agoraEntity) flatMap {
        // Can create entities that do not exist
        case false => DBIO.successful(AgoraPermissions(Create))
        case true => lookupPermissions(agoraEntity, userEmail)
      }
    }
  }

  def listOwners(agoraEntity: AgoraEntity)
                (implicit executionContext: ExecutionContext): ReadWriteAction[Seq[String]] = {
    withPermissionNotFoundException("Couldn't find any managers.") {
      val permissionsQuery = for {
        entity <- entities if entity.alias === alias(agoraEntity)
        permission <- permissions if permission.entityID === entity.id && (permission.roles >= AgoraPermissions.Manage)
        user <- users if user.id === permission.userID
      } yield user.email

      permissionsQuery.result
    }
  }

  def listPermissions(agoraEntity: AgoraEntity)
                     (implicit executionContext: ExecutionContext): ReadWriteAction[Seq[AccessControl]] = {
    withPermissionNotFoundException("Could not list permissions") {
      // Construct query
      val permissionsQuery = for {
        entity <- entities if entity.alias === alias(agoraEntity)
        _permissions <- permissions if _permissions.entityID === entity.id
        user <- users if user.id === _permissions.userID
      } yield (user.email, _permissions.roles)

      permissionsQuery.result map { accessObjects: Seq[(String, Int)] =>
         accessObjects.map(AccessControl.apply)
      }
    }
  }

  def insertPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl)
                      (implicit executionContext: ExecutionContext): ReadWriteAction[Int] = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    addUserIfNotInDatabase(userEmail) flatMap { _ =>
      // construct insert action
      val addPermissionAction = for {
        user <- users
          .filter(_.email === userEmail)
          .result
          .head

        entity <- entities
          .filter(_.alias === alias(agoraEntity))
          .result
          .head

        result <- permissions += PermissionDao(user.id.get, entity.id.get, roles.toInt)
      } yield result

      addPermissionAction.asTry flatMap {
        case Success(yay) => DBIO.successful(yay)
        case Failure(_) => editPermission(agoraEntity, userAccessObject)
      }
    }
  }

  def editPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl)
                    (implicit executionContext: ExecutionContext): ReadWriteAction[Int] = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    roles match {
      case AgoraPermissions(Nothing) =>
        deletePermission(agoraEntity, userEmail)
      case _ =>
        addUserIfNotInDatabase(userEmail) flatMap { _ =>
          withPermissionNotFoundException("Could not edit permission") {
            // construct update action
            val permissionsUpdateAction = for {
              user <- users
                .filter(_.email === userEmail)
                .result
                .head

              entity <- entities
                .filter(_.alias === alias(agoraEntity))
                .result
                .head

              permission <- permissions
                .filter(p => p.entityID === entity.id && p.userID === user.id)
                .map(_.roles)
                .update(roles.toInt)
            } yield permission

            permissionsUpdateAction flatMap { rowsEdited =>
              if (rowsEdited == 0) {
                DBIO.failed(new Exception("No rows were edited."))
              } else {
                DBIO.successful(rowsEdited)
              }
            }
          }
        }
      }
    }

  def deletePermission(agoraEntity: AgoraEntity, userToRemove: String)
                      (implicit executionContext: ExecutionContext): ReadWriteAction[Int] = {
    addUserIfNotInDatabase(userToRemove) flatMap { _ =>
      withPermissionNotFoundException("Could not delete permission") {
        // construct update action
        val permissionsUpdateAction = for {
          user <- users
            .filter(_.email === userToRemove)
            .result
            .head

          entity <- entities
            .filter(_.alias === alias(agoraEntity))
            .result
            .head

          result <- permissions
            .filter(p => p.entityID === entity.id && p.userID === user.id)
            .delete
          
        } yield result

        permissionsUpdateAction flatMap { rowsEdited =>
          if (rowsEdited == 0) {
            DBIO.failed(new Exception("No rows were deleted."))
          } else {
            DBIO.successful(rowsEdited)
          }
        }
      }
    }
  }

  def deleteAllPermissions(agoraEntity: AgoraEntity)
                          (implicit executionContext: ExecutionContext): ReadWriteAction[Int] = {
    withPermissionNotFoundException("Could not delete permissions") {
      for {
        entity <- entities
          .filter(_.alias === alias(agoraEntity))
          .result
          .head

        rowsDeleted <- permissions
          .filter(_.entityID === entity.id)
          .delete
      } yield rowsDeleted
    }
  }


  /** The list of entity aliases this user has at-least read access to, including public entities.
    *
    * @param userEmail the user for which to query
    * @param entityAliases the list of entity aliases to consider for the query
    * @return entity aliases
    */
  def listReadableEntities(userEmail: String, entityAliases: Option[Seq[String]] = None): ReadAction[Seq[String]] = {
    val aliasQuery = for {
      user <- users if user.email.inSetBind(List(userEmail, AccessControl.publicUser))
      // see AgoraPermissions; roles will always be odd if the user has read perms, because bitmasks.
      permission <- permissions if permission.userID === user.id && permission.roles.%(2) === 1
      entity <- entities if permission.entityID === entity.id
    } yield entity.alias

    val filteredQuery = entityAliases match {
      case Some(aliases) =>
        metricsClient.recordMetric("mysql", JsObject(
          "aliasInClauseSize" -> JsNumber(aliases.size)
        ))
        aliasQuery.filter(_.inSetBind(aliases))
      case None => aliasQuery
    }

    filteredQuery.result
  }

  // be careful with this method. Many previous callers of this method were very inefficient, retrieving entire
  // collections from Mongo and then filtering out 90%+ of those documents, leading to scale issues.
  // We are not deprecating or removing this method because it is appropriate for certain use cases.
  def filterEntityByRead(agoraEntities: Seq[AgoraEntity], userEmail: String, callerTag: String = "unknown")
                        (implicit executionContext: ExecutionContext): ReadAction[Seq[AgoraEntity]] = {

    if (agoraEntities.isEmpty) {
      // short-circuit: if we're asked to filter the empty set, don't go to mysql. Just return the empty set.
      DBIO.successful(Seq.empty[AgoraEntity])

    } else  {

      // The maximum number of entity aliases we would send to mysql for an "IN" clause.
      // The primary use case this targets is AgoraBusiness.findSingle(), which retrieves one entity from Mongo
      // and then calls this method to check permissions. When we have one Mongo entity, we really don't want
      // to get ALL permissions from MySQL.
      val entityFilterMaxCount = AgoraConfig.sqlAliasBatchSize // defaults to 100

      val entityAliases = if (agoraEntities.nonEmpty && agoraEntities.size < entityFilterMaxCount) {
        Option(agoraEntities.map(alias))
      } else {
        logger.info(s"filterEntityByRead bypassing IN clause because it found ${agoraEntities.size}/$entityFilterMaxCount entities")
        metricsClient.recordMetric("mysql", JsObject(
          "inClauseOverLimit" -> JsNumber(agoraEntities.size),
          "inClauseMaxSize" -> JsNumber(entityFilterMaxCount)
        ))
        None
      }

      listReadableEntities(userEmail, entityAliases) map { aliasedAgoraEntitiesWithReadPermissions =>
        val aliasedAgoraEntitiesWithReadPermissionsSet = aliasedAgoraEntitiesWithReadPermissions.toSet
        val filteredEntities = agoraEntities.filter(agoraEntity =>
          aliasedAgoraEntitiesWithReadPermissionsSet.contains(alias(agoraEntity))
        )

        // metrics on how efficient this operation is: of all the Mongo entities supplied in arguments,
        // how many are filtered out due to permissions?
        val rawCount = agoraEntities.size
        val filteredCount = filteredEntities.size
        val efficiency:Float = filteredCount.toFloat / rawCount.toFloat
        metricsClient.recordMetric("queryEfficiency", JsObject(
          "caller" -> JsString(s"$callerTag"),
          "method" -> JsString("filterEntityByRead"),
          "efficiency" -> JsNumber(efficiency.toDouble),
          "filtered" -> JsNumber(filteredCount.toDouble),
          "raw" -> JsNumber(rawCount)
        ))

        filteredEntities
      }
    }
  }

  def listPublicAliases(): ReadAction[Seq[String]] = {
    val publicAliasQuery = for {
      user <- users if user.email === AccessControl.publicUser
      permission <- permissions if permission.userID === user.id && permission.roles > 0
      entity <- entities if permission.entityID === entity.id
    } yield entity.alias

    publicAliasQuery.result
  }

  def listOwnersAndAliases(): ReadAction[Seq[(String,String)]] = {
    val ownerAndAliasQuery = for {
      user <- users
      permission <- permissions if permission.userID === user.id && (permission.roles === Manage || permission.roles === All)
      entity <- entities if permission.entityID === entity.id
    } yield (entity.alias, user.email)

    ownerAndAliasQuery.result
  }

  def sqlDBStatus()(implicit executionContext: ExecutionContext): ReadAction[Unit] = {
    //noinspection SqlDialectInspection
    for {
      _ <- DBIO.successful(logger.info("Checking database version"))
      _ <- sql"select version();".as[String]
      _ <- DBIO.successful(logger.info("Checked database version"))
    } yield ()
  }
}
