
package org.broadinstitute.dsde.agora.server

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{AgoraMongoClient, EmbeddedMongo}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import slick.dbio.DBIOAction
import slick.dbio.Effect.Read
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait AgoraTestFixture {
  import scala.concurrent.ExecutionContext.Implicits.global

  val db: Database = AgoraConfig.sqlDatabase.db
  val permsDataSource: PermissionsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)
  val agoraBusiness: AgoraBusiness = new AgoraBusiness(permsDataSource)
  val permissionBusiness: PermissionBusiness = new PermissionBusiness(permsDataSource)

  def startDatabases() = {
    EmbeddedMongo.startMongo()

    clearDatabases()
    val setupFuture = createTableIfNotExists(users, entities, permissions)

    println("Populating sql database.")
    Await.result(setupFuture, timeout)
    println("Finished populating sql database.")
  }

  def stopDatabases() = {
    clearDatabases()
    EmbeddedMongo.stopMongo()
    println("Disconnecting from sql database.")
    db.close()
  }

  def ensureMongoDatabaseIsRunning() = {
    if (!EmbeddedMongo.isRunning) {
      startDatabases()
    }
  }

  def clearMongoCollections(collections: Seq[MongoCollection] = Seq()) = {
    if (EmbeddedMongo.isRunning) {
      println("Clearing mongo database.")
      val allCollections = AgoraMongoClient.getAllCollections ++ collections
      allCollections.foreach(collection => {
        collection.remove(MongoDBObject.empty)
      })
    }
  }

  private def createTableIfNotExists(tables: TableQuery[_ <: Table[_]]*) = {
    val actions = tables map { table =>
      MTable.getTables(table.baseTableRow.tableName).flatMap { result =>
        if (result.isEmpty) {
          table.schema.create
        } else {
          DBIOAction.successful(())
        }
      }
    }
    db.run(DBIO.sequence(actions))
  }

  private def deleteFromTableIfExists(tables: TableQuery[_ <: Table[_]]*): Future[Seq[AnyVal]] = {
    val actions = tables map { table =>
      MTable.getTables(table.baseTableRow.tableName).flatMap { result =>
        if (result.nonEmpty) {
          sqlu"delete from #${table.baseTableRow.tableName}"
        } else {
          DBIOAction.successful(())
        }
      }
    }
    db.run(DBIOAction.sequence(actions))
  }

  def ensureSqlDatabaseIsRunning() = {
    println("Populating sql database.")
    Await.result(createTableIfNotExists(entities, users, permissions), timeout)
  }

  def clearSqlDatabase() = {
    println("Clearing sql database.")
    Await.result(deleteFromTableIfExists(permissions), timeout)
    Await.result(deleteFromTableIfExists(users), timeout)
    Await.result(deleteFromTableIfExists(entities), timeout)
  }

  def ensureDatabasesAreRunning() = {
    ensureMongoDatabaseIsRunning()
    ensureSqlDatabaseIsRunning()
  }

  def clearDatabases() = {
    clearMongoCollections()
    clearSqlDatabase()
  }

  def addAdminUser() = {
    Await.result(db.run(users += UserDao(adminUser.get, isAdmin = true)), timeout)
  }

  protected def patiently[R](op: => Future[R], duration: Duration = 1 minutes): R = {
    Await.result(op, duration)
  }

  protected def runInDB[R](action: DataAccess => DBIOAction[R, _ <: NoStream, _ <: Effect], duration: Duration = 1 minutes): R = {
    patiently(permsDataSource.inTransaction { db => action(db).asInstanceOf[ReadWriteAction[R]] }, duration)
  }
}
