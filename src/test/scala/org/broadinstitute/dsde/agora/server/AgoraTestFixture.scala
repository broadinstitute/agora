
package org.broadinstitute.dsde.agora.server

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{AgoraMongoClient, EmbeddedMongo}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{UserDao, entities, permissions, users}
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait AgoraTestFixture {
  val timeout = 10.seconds
  var db: Database = _

  def startDatabases() = {
    EmbeddedMongo.startMongo()

    println("Connecting to test sql database.")
    db = AgoraConfig.sqlDatabase.db
    clearDatabases()

    val setupFuture = createTableIfNotExists(entities, permissions, users)

    println("Populating sql database.")
    Await.result(setupFuture, timeout)
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

  private def createTableIfNotExists(tables: TableQuery[_ <: Table[_]]*): Future[Seq[Unit]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future.sequence(
      tables map { table =>
        db.run(MTable.getTables(table.baseTableRow.tableName)).flatMap { result =>
          if (result.isEmpty) {
            db.run(sqlu"delete from #${table.baseTableRow.tableName}")
          } else {
            Future.successful(())
          }
        }
      }
    )
  }

  private def deleteFromTableIfExists(tables: TableQuery[_ <: Table[_]]*): Future[Seq[AnyVal]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future.sequence(
      tables map { table =>
        db.run(MTable.getTables(table.baseTableRow.tableName)).flatMap { result =>
          if (result.nonEmpty) {
            db.run(sqlu"delete from #${table.baseTableRow.tableName}")
          } else {
            Future.successful(())
          }
        }
      }
    )
  }

  def ensureSqlDatabaseIsRunning() = {
    println("Connecting to test sql database.")
    db = AgoraConfig.sqlDatabase.db

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
}
