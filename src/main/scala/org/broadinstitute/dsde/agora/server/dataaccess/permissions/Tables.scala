package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import slick.jdbc.MySQLProfile.api._

// Users
case class UserDao(email: String, isAdmin: Boolean = false, id: Option[Int] = None)

class UsersTable(tag: Tag) extends Table[UserDao](tag, "USERS") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def email = column[String]("EMAIL", O.Length(255))
  def is_admin = column[Boolean]("IS_ADMIN")

  // This * projection auto-transforms the tupled column
  // values to and from a User.
  def * = (email, is_admin, id.?) <> (UserDao.tupled, UserDao.unapply)
  def idx = index("idx_users", email, unique = true)
}

object users extends TableQuery(new UsersTable(_)) {
  val findByEmail= this.findBy(_.email)
}

// Entities
case class EntityDao(alias: String, id: Option[Int] = None) {}

class EntitiesTable(tag: Tag) extends Table[EntityDao](tag, "ENTITIES") {
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def alias = column[String]("ALIAS", O.Length(255))

  def * = (alias, id.?) <> (EntityDao.tupled, EntityDao.unapply)
  def idx = index("idx_entitys", alias, unique = true)
}

object entities extends TableQuery(new EntitiesTable(_)) {
  val findByAlias = this.findBy(_.alias)
}

// Permissions
case class PermissionDao(userID: Int, entityID: Int, roles: Int)

class PermissionsTable(tag: Tag) extends Table[PermissionDao](tag, "PERMISSIONS") {
  def userID = column[Int]("USER_ID")
  def entityID = column[Int]("ENTITY_ID")
  def roles = column[Int]("ROLE")

  def _primaryKey = primaryKey("pk_permissions", (userID, entityID))
  def * = (userID, entityID, roles) <> (PermissionDao.tupled, PermissionDao.unapply)

  def userForeignKey = foreignKey("USER_FK", userID, users)(_.id)
  def entityForeignKey = foreignKey("ENTITY_FK", entityID, entities)(_.id)
}

object permissions extends TableQuery(new PermissionsTable(_)) {
}

