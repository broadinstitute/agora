package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import AgoraPermissions._
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.{ReadAction, ReadWriteAction, WriteAction}
import org.broadinstitute.dsde.agora.server.exceptions.PermissionNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.MySQLDriver.api._
import slick.jdbc.SQLInterpolation

import scala.util.{Failure, Success, Try}

trait PermissionsClient {

  val db = AgoraConfig.sqlDatabase.db
  val driver: JdbcProfile = AgoraConfig.sqlDatabase.driver
  import driver.api._

  val timeout = 10.seconds

  def alias(entity: AgoraEntity): String

  def withPermissionNotFoundException[T, E](errorString: String = "Could not get permission")( op: => DBIOAction[T, NoStream, E with Effect] ): DBIOAction[T, NoStream, E with Effect] = {
    op.asTry flatMap {
      case Success(permission) => DBIO.successful(permission)
      case Failure(ex) => DBIO.failed(new PermissionNotFoundException(errorString, ex))
    }
  }

  // Users
  def addUserIfNotInDatabase(userEmail: String): WriteAction[Int] = {
    // Attempts to add user to UserTable and ignores errors if user already exists
    (users += UserDao(userEmail)) cleanUp { failOpt => DBIO.successful(0) }
  }

  def isAdmin(userEmail: String): ReadAction[Boolean] = {
    users.findByEmail(userEmail).result map { aa =>
      aa.head.isAdmin
    }
  }

  def listAdmins(): ReadAction[Seq[String]] = {
    withPermissionNotFoundException() {
      val adminsQuery = for {
        user <- users if user.is_admin === true
      } yield user.email

      adminsQuery.result
    }
  }

  def updateAdmin(userEmail: String, adminStatus: Boolean) = {
    withPermissionNotFoundException(s"Could not make user ${userEmail} admin") {
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

  def doesEntityExists(agoraEntity: AgoraEntity): ReadAction[Boolean] = {
    entities.findByAlias(alias(agoraEntity)).result map { entity =>
      entity.nonEmpty
    }
  }

  private def permissionQuery(agoraEntity: AgoraEntity, userEmail: String): ReadAction[Option[PermissionDao]] = {
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


  private def lookupPermissions(agoraEntity: AgoraEntity, userEmail: String): ReadWriteAction[AgoraPermissions] = {
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
  def getPermission(agoraEntity: AgoraEntity, userEmail: String): ReadWriteAction[AgoraPermissions] = {
    withPermissionNotFoundException() {
      doesEntityExists(agoraEntity) flatMap {
        // Can create entities that do not exist
        case false => DBIO.successful(AgoraPermissions(Create))
        case true => lookupPermissions(agoraEntity, userEmail)
      }
    }
  }

  def listOwners(agoraEntity: AgoraEntity): ReadAction[Seq[String]] = {
    withPermissionNotFoundException("Couldn't find any managers.") {
      val permissionsQuery = for {
        entity <- entities if entity.alias === alias(agoraEntity)
        permission <- permissions if permission.entityID === entity.id && (permission.roles >= AgoraPermissions.Manage)
        user <- users if user.id === permission.userID
      } yield user.email

      permissionsQuery.result
    }
  }

  def listPermissions(agoraEntity: AgoraEntity): ReadAction[Seq[AccessControl]] = {
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

  def insertPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl): ReadWriteAction[Int] = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    addUserIfNotInDatabase(userEmail) flatMap { added =>
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
        case Failure(nooo) => editPermission(agoraEntity, userAccessObject)
      }
    }
  }

  def editPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl): ReadWriteAction[Int] = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    roles match {
      case AgoraPermissions(Nothing) =>
        deletePermission(agoraEntity, userEmail)
      case _ =>
        addUserIfNotInDatabase(userEmail) flatMap { added =>
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

  def deletePermission(agoraEntity: AgoraEntity, userToRemove: String): ReadWriteAction[Int] = {
    addUserIfNotInDatabase(userToRemove) flatMap { added =>
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
            DBIO.failed(new Exception("No rows were edited."))
          } else {
            DBIO.successful(rowsEdited)
          }
        }
      }
    }
  }

  def deleteAllPermissions(agoraEntity: AgoraEntity): ReadWriteAction[Int] = {
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

  def filterEntityByRead(agoraEntities: Seq[AgoraEntity], userEmail: String) = {
    val entitiesThatUserCanReadQuery = for {
      user <- users if user.email === userEmail || user.email === "public"
      permission <- permissions if permission.userID === user.id && (
        permission.roles === 1 || permission.roles === 3 || permission.roles === 5 || permission.roles === 7 || permission.roles === 9 ||
        permission.roles === 11 || permission.roles === 13 || permission.roles === 15 || permission.roles === 17 || permission.roles === 19 ||
        permission.roles === 21 || permission.roles === 23 || permission.roles === 25 || permission.roles === 27 || permission.roles === 29 ||
        permission.roles === 31)
      entity <- entities if permission.entityID === entity.id
    } yield entity

    entitiesThatUserCanReadQuery.result map { entitiesThatCanBeRead =>
      entitiesThatCanBeRead.map(_.alias)
    } map { aliasedAgoraEntitiesWithReadPermissions =>
      agoraEntities.filter(agoraEntity =>
        aliasedAgoraEntitiesWithReadPermissions.contains(alias(agoraEntity))
      )
    }
  }

  def sqlDBStatus() = {
    sql"select version();".as[String].transactionally
  }
}
