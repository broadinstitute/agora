
package org.broadinstitute.dsde.agora.server

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{AgoraMongoClient, EmbeddedMongo}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{UserDao, entities, permissions, users}
import slick.driver.MySQLDriver.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait AgoraTestFixture extends LazyLogging {
  val timeout: FiniteDuration = 10.seconds
  var db: Database = _

  def startDatabases(): Seq[Unit] = {
    EmbeddedMongo.startMongo()

    logger.info("Connecting to test sql database.")
    db = AgoraConfig.sqlDatabase
    clearDatabases()

    val setupFuture = createTableIfNotExists(entities, permissions, users)

    logger.info("Populating sql database.")
    Await.result(setupFuture, timeout)
  }

  def stopDatabases(): Unit = {
    clearDatabases()
    EmbeddedMongo.stopMongo()
    logger.info("Disconnecting from sql database.")
    db.close()
  }

  def ensureMongoDatabaseIsRunning(): Any = {
    if (!EmbeddedMongo.isRunning) {
      startDatabases()
    }
  }

  def clearMongoCollections(collections: Seq[MongoCollection] = Seq()): Unit = {
    if (EmbeddedMongo.isRunning) {
      logger.info("Clearing mongo database.")
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
            db.run(table.schema.create)
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
            db.run(table.delete)
          } else {
            Future.successful(())
          }
        }
      }
    )
  }

  def ensureSqlDatabaseIsRunning(): Seq[Unit] = {
    logger.info("Connecting to test sql database.")
    db = AgoraConfig.sqlDatabase

    logger.info("Populating sql database.")
    Await.result(createTableIfNotExists(entities, users, permissions), timeout)
  }

  def clearSqlDatabase(): Seq[AnyVal] = {
    logger.info("Clearing sql database.")
    Await.result(deleteFromTableIfExists(permissions), timeout)
    Await.result(deleteFromTableIfExists(users), timeout)
    Await.result(deleteFromTableIfExists(entities), timeout)
  }

  def ensureDatabasesAreRunning(): Seq[Unit] = {
    ensureMongoDatabaseIsRunning()
    ensureSqlDatabaseIsRunning()
  }

  def clearDatabases(): Seq[AnyVal] = {
    clearMongoCollections()
    clearSqlDatabase()
  }

  def addAdminUser(): Int = {
    Await.result(db.run(users += UserDao(adminUser.get, isAdmin = true)), timeout)
  }
}
