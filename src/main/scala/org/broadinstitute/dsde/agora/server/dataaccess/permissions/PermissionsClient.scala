package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import AgoraPermissions._
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.exceptions.PermissionNotFoundException
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import scala.concurrent.{Future, Await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import slick.driver.MySQLDriver.api._

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

    val permissionsQuery = for {
      user <- users if user.email === userEmail
      entity <- entities if entity.alias === alias(agoraEntity)
      permission <- permissions if permission.entityID === entity.id &&
                                    permission.userID === user.id
    } yield permission

    val permissionsFuture = db.run(permissionsQuery.result)

    val agoraPermissions = permissionsFuture.map { permissionsResult =>
      val permissionResult = permissionsResult.headOption

      if (permissionResult.isEmpty)
        AgoraPermissions(Nothing)
      else
        AgoraPermissions(permissionResult.get.roles)

    } recover {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not get permissions", ex)
    }

    Await.result(agoraPermissions, timeout)
  }

  def listPermissions(agoraEntity: AgoraEntity): Seq[AccessControl] = {
    val permissionsQuery = for {
      entity <- entities if entity.alias === alias(agoraEntity)
      _permissions <- permissions if _permissions.entityID === entity.id
      user <- users if user.id === _permissions.userID
    } yield (user.email, _permissions.roles)

    val permissionsFuture = db.run(permissionsQuery.result)

    val accessControls = permissionsFuture.map { accessObjects: Seq[(String, Int)] =>
      accessObjects.map(AccessControl.apply)

    } recover {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not list permissions", ex)
    }

    Await.result(accessControls, timeout)
  }

  def insertPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl): Int = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    addUserIfNotInDatabase(userEmail)

    val userAndEntityQuery = for {
      user <- users if user.email === userEmail
      entity <- entities if entity.alias === alias(agoraEntity)
    } yield (user.id, entity.id)

    val permissionsFuture = db.run(userAndEntityQuery.result)

    val addPermissionFuture = permissionsFuture.flatMap { permissionsResult =>
      val userAndEntity = permissionsResult.headOption

      if (userAndEntity.isDefined) {
        val userID = userAndEntity.get._1
        val entityID = userAndEntity.get._2

        // First, try to update
        // if permission does not exist, update will fail by updating 0 rows
        val updateQuery = permissions
          .filter(_.userID === userID)
          .filter(_.entityID === entityID)
          .map(_.roles)

        val updateAction = updateQuery.update(roles.toInt)
        val rowsEdited = Await.result(db.run(updateAction), timeout)

        // If no rows were updated, then insert a new Permission
        if (rowsEdited == 0)
          db.run(permissions += PermissionDao(userID, entityID, roles.toInt))
        else
          Future(rowsEdited)
      }

      else
        throw new Exception(s"Could not find either entity: $agoraEntity or user: $userEmail while trying to insert permissions.")
    } recover {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not insert permissions", ex)
    }

    Await.result(addPermissionFuture, timeout)
  }

  def editPermission(agoraEntity: AgoraEntity, userAccessObject: AccessControl): Int = {
    val userEmail = userAccessObject.user
    val roles = userAccessObject.roles

    addUserIfNotInDatabase(userEmail)

    val permissionsQuery = for {
      user <- users if user.email === userEmail
      entity <- entities if entity.alias === alias(agoraEntity)
    } yield (user.id, entity.id)

    val permissionsFuture = db.run(permissionsQuery.result)

    val editPermissionFuture = permissionsFuture.flatMap { permissionsResult =>
      val userAndEntity = permissionsResult.headOption

      if (userAndEntity.isDefined) {
        val userID = userAndEntity.get._1
        val entityID = userAndEntity.get._2

        val updateQuery = permissions
          .filter(_.userID === userID)
          .filter(_.entityID === entityID)
          .map(_.roles)

        val updateAction = updateQuery.update(roles.toInt)
        db.run(updateAction)
      }

      else
        throw new Exception(s"Could not find permissions for user: $userEmail on entity: $agoraEntity while trying to edit permissions.")
    } recover {
      case ex: Throwable => throw new PermissionNotFoundException(s"Could not edit permissions", ex)
    }

    val rowsEdited = Await.result(editPermissionFuture, timeout)

    // If no rows were changed, then the permission could not be found.
    if (rowsEdited == 0)
      throw new PermissionNotFoundException("Could not find permission to edit.")
    else
      rowsEdited
  }

  def deletePermission(entity: AgoraEntity, userToRemove: String): Int = {
    val accessObject = AccessControl(userToRemove, AgoraPermissions(Nothing))
    editPermission(entity, accessObject)
  }

  def filterEntityByRead(agoraEntities: Seq[AgoraEntity], userEmail: String) = {
    val entitiesThatUserCanReadQuery = for {
      user <- users if user.email === userEmail
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

}
