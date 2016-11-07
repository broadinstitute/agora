package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import AgoraPermissions._
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.exceptions.PermissionNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.MySQLDriver.api._
import slick.jdbc.SQLInterpolation

import scala.util.Try

trait PermissionsClient {

  val db = AgoraConfig.sqlDatabase
  val timeout = 10.seconds

  def alias(entity: AgoraEntity): String

  // Users
  def addUserIfNotInDatabase(userEmail: String): Unit = {
    // Attempts to add user to UserTable and ignores errors if user already exists
    try {
      Await.ready(db.run(users += UserDao(userEmail)), timeout)
    } catch {
      case _ : Throwable => //Do nothing
    }
  }

  def isAdmin(userEmail: String): Boolean = {
    val userQuery = users.findByEmail(userEmail)
    val user = Await.result(db.run(userQuery.result.head), timeout)
    user.isAdmin
  }

  def listAdmins(): Seq[String] = {
    val adminsQuery = for {
      user <- users if user.is_admin === true
    } yield user.email

    try {
      Await.result(db.run(adminsQuery.result), timeout)
    } catch {
      case ex: Throwable =>
        throw new PermissionNotFoundException(ex.getMessage, ex)
    }
  }

  def updateAdmin(userEmail: String, adminStatus: Boolean) = {
    addUserIfNotInDatabase(userEmail)

    // construct update action
    val adminsUpdateAction = for {
      user <- users
        .filter(_.email === userEmail)
        .map(_.is_admin)
        .update(adminStatus)
    } yield user

    // run update action
    try {
      val rowsEdited = Await.result(db.run(adminsUpdateAction), timeout)

      if (rowsEdited == 0)
        throw new Exception("No rows were edited.")
      else
        rowsEdited

    } catch {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not make user ${userEmail} admin", ex)
    }
  }

  // Entities
  def addEntity(entity: AgoraEntity): Future[Int] =
    Await.ready(db.run(entities += EntityDao(alias(entity))), timeout)

  def doesEntityExists(agoraEntity: AgoraEntity): Boolean = {
    val entityQuery = db.run(entities.findByAlias(alias(agoraEntity)).result)
    val entity = Await.result(entityQuery, timeout)
    entity.nonEmpty
  }

  // Permissions
  def getPermission(agoraEntity: AgoraEntity, userEmail: String): AgoraPermissions = {

    // Can create entities that do not exist
    if (!doesEntityExists(agoraEntity))
      return AgoraPermissions(Create)

    addUserIfNotInDatabase(userEmail)

    // Construct query to get permissions
    val permissionsQuery = for {
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

    // run query
    try {
      val permissionResult = Await.result(db.run(permissionsQuery), timeout)

      if (permissionResult.isEmpty)
        AgoraPermissions(Nothing)
      else
        AgoraPermissions(permissionResult.get.roles)

    } catch {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not get permission", ex)
    }

  }

  def listOwners(agoraEntity: AgoraEntity): Seq[String] = {
    // you're a manager if your role & (1 << 4) != 0 (and I think we're in the range from 0-32) so basically 16-31 inclusive
    val permissionsQuery = for {
      entity <- entities if entity.alias === alias(agoraEntity)
      _permissions <- permissions if _permissions.entityID === entity.id && (_permissions.roles inSetBind List.range(16, 32))
      user <- users if user.id === _permissions.userID
    } yield user.email
    try {
      Await.result(db.run(permissionsQuery.result), timeout)
    } catch {
      case ex: Throwable => throw new PermissionNotFoundException(s"Couldn't find any managers. ", ex)
    }
  }

  def listPermissions(agoraEntity: AgoraEntity): Seq[AccessControl] = {
    // Construct query
    val permissionsQuery = for {
      entity <- entities if entity.alias === alias(agoraEntity)
      _permissions <- permissions if _permissions.entityID === entity.id
      user <- users if user.id === _permissions.userID
    } yield (user.email, _permissions.roles)

    // Get Future of the query result
    val permissionsFuture = db.run(permissionsQuery.result)

    // if successful, map the Future
    val accessControls = permissionsFuture.map { accessObjects: Seq[(String, Int)] =>
      accessObjects.map(AccessControl.apply)

    // if unsuccessful, throw exception
    } recover {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not list permissions", ex)
    }

    Await.result(accessControls, timeout)
  }

  def insertPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl): Int = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    addUserIfNotInDatabase(userEmail)

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

    // run insert action
    try {
      Await.result(db.run(addPermissionAction), timeout)
    } catch {
      case ex:Throwable => editPermission(agoraEntity, userAccessObject)
    }
  }

  def editPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl): Int = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    roles match {
      case AgoraPermissions(Nothing) =>
        deletePermission(agoraEntity, userEmail)
      case _ =>
        addUserIfNotInDatabase(userEmail)

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

        // run update action
        try {
          val rowsEdited = Await.result(db.run(permissionsUpdateAction), timeout)

          if (rowsEdited == 0)
            throw new Exception("No rows were edited.")
          else
            rowsEdited

        } catch {
          case ex: Throwable => throw new PermissionNotFoundException(s"Could not edit permission", ex)
        }
    }
  }

  def deletePermission(agoraEntity: AgoraEntity, userToRemove: String): Int = {
    addUserIfNotInDatabase(userToRemove)

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

    // run update action
    try {
      val rowsEdited = Await.result(db.run(permissionsUpdateAction), timeout)

      if (rowsEdited == 0)
        throw new Exception("No rows were edited.")
      else
        rowsEdited

    } catch {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not delete permission", ex)
    }
  }

  def deleteAllPermissions(agoraEntity: AgoraEntity): Int = {
    val deleteQuery = for {
      entity <- entities
        .filter(_.alias === alias(agoraEntity))
        .result
        .head

      rowsDeleted <- permissions
        .filter(_.entityID === entity.id)
        .delete
    } yield rowsDeleted

    try {
      Await.result(db.run(deleteQuery), timeout)
    } catch {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not delete permissions", ex)
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

    val readableEntities = db.run(entitiesThatUserCanReadQuery.result)

    val readableEntitiesFuture = readableEntities.map { entitiesThatCanBeRead =>
      entitiesThatCanBeRead.map(_.alias)
    }

    val aliasedAgoraEntitiesWithReadPermissions = Await.result(readableEntitiesFuture, timeout)

    agoraEntities.filter(agoraEntity =>
      aliasedAgoraEntitiesWithReadPermissions.contains(alias(agoraEntity))
    )
  }

  def sqlDBStatus(): Try[Unit] = {
    val action = sql"select version();".as[String]
    Try(Await.result(db.run(action.transactionally), timeout))
  }
}
