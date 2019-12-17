
package org.broadinstitute.dsde.agora.server

import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.util.Timeout
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.actor.AgoraGuardianActor
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessExecutionContext, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.dataaccess.health.HealthMonitorSubsystems
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{AgoraMongoClient, EmbeddedMongo}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.{BodyWithContentType, HttpRequest, StringBody}
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

trait AgoraTestFixture extends LazyLogging {
  this: AgoraAkkaTest =>

  implicit lazy val agoraBusinessExecutionContext: AgoraBusinessExecutionContext =
    new AgoraBusinessExecutionContext(executor)

  val timeout: FiniteDuration = 10.seconds
  val db: Database = AgoraConfig.sqlDatabase.db
  val permsDataSource: PermissionsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)
  val permissionBusiness: PermissionBusiness = new PermissionBusiness(permsDataSource)

  lazy val agoraGuardian: ActorRef[AgoraGuardianActor.Command] = {
    // Every val using agoraGuardian outside of beforeAll should be lazy themselves...
    // But coders forget, and then the guardian spins up before the database is ready
    ensureDatabasesAreRunning()
    actorTestKit.spawn(
      AgoraGuardianActor(permsDataSource, new HealthMonitorSubsystems(Map.empty), DispatcherSelector.sameAsParent())
    )
  }

  lazy val agoraBusiness: AgoraBusiness = new AgoraBusiness(permsDataSource, agoraGuardian)

  var waasMockServer: ClientAndServer = _
  val waasRequest: HttpRequest = request()
    .withMethod("POST")
    .withPath("/api/womtool/v1/describe")

  val mockAccessToken: String = AgoraConfig.mockAccessToken

  def startMockWaas(): Unit = {
    waasMockServer = startClientAndServer(waasMockServerPort)
    addSubstringResponse(payload1.get, payload1DescribeResponse)
    addSubstringResponse(payload2.get, payload2DescribeResponse)
    addSubstringResponse(payloadWithValidPersonalDockerImageInWdl.get, genericDockerPayloadDescribeResponse)
    addSubstringResponse(payloadWithValidOfficialDockerImageInWdl.get, genericDockerPayloadDescribeResponse)
    addSubstringResponse(payloadWdlBadVersion.get, badVersionDescribeResponse)
    addSubstringResponse(badPayload.get, malformedPayloadDescribeResponse)
    addSubstringResponse(payloadWdl10.get, goodWdlVersionDescribeResponse)
  }

  def addSubstringResponse(payload: String, response: String): Unit = {
    // necessary to declare this here in order to disambiguate overloaded method
    val p2 : BodyWithContentType[String] = new StringBody(payload, true)
    waasMockServer.when(
      waasRequest
        .withBody(p2)
        )
      .respond(
        org.mockserver.model.HttpResponse.response().withBody(response).withStatusCode(OK.intValue).withHeader("Content-Type", "application/json"))

  }

  def stopMockWaas(): Unit = {
    waasMockServer.stop()
  }

  def startDatabases(): Unit = {
    EmbeddedMongo.startMongo()

    clearDatabases()
    val setupFuture = createTableIfNotExists(users, entities, permissions)

    logger.debug("Populating sql database.")
    Await.result(setupFuture, timeout)
    logger.debug("Finished populating sql database.")
  }

  def stopDatabases(): Unit = {
    clearDatabases()
    EmbeddedMongo.stopMongo()
    logger.debug("Disconnecting from sql database.")
    db.close()
  }

  def ensureMongoDatabaseIsRunning(): Unit = {
    if (!EmbeddedMongo.isRunning) {
      startDatabases()
    }
  }

  def clearMongoCollections(collections: Seq[MongoCollection] = Seq()): Unit = {
    if (EmbeddedMongo.isRunning) {
      logger.debug("Clearing mongo database.")
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
          //noinspection SqlDialectInspection
          sqlu"delete from #${table.baseTableRow.tableName}"
        } else {
          DBIOAction.successful(())
        }
      }
    }
    db.run(DBIOAction.sequence(actions))
  }

  def ensureSqlDatabaseIsRunning(): Unit = {
    logger.debug("Populating sql database.")
    Await.result(createTableIfNotExists(entities, users, permissions), timeout)
  }

  def clearSqlDatabase(): Unit = {
    logger.debug("Clearing sql database.")
    Await.result(deleteFromTableIfExists(permissions), timeout)
    Await.result(deleteFromTableIfExists(users), timeout)
    Await.result(deleteFromTableIfExists(entities), timeout)
  }

  def ensureDatabasesAreRunning(): Unit = {
    ensureMongoDatabaseIsRunning()
    ensureSqlDatabaseIsRunning()
  }

  def clearDatabases(): Unit = {
    clearMongoCollections()
    clearSqlDatabase()
  }

  def addAdminUser(): Unit = {
    Await.result(db.run(users += UserDao(adminUser.get, isAdmin = true)), timeout)
  }

  protected def patiently[R](op: => Future[R], duration: Duration = 1 minutes): R = {
    Await.result(op, duration)
  }

  protected def runInDB[R](action: DataAccess => DBIOAction[R, _ <: NoStream, _ <: Effect], duration: Duration = 1 minutes): R = {
    patiently(permsDataSource.inTransaction { db => action(db).asInstanceOf[ReadWriteAction[R]] }, duration)
  }

  protected def flushAgoraDataCache(): Unit = {
    implicit val flushTimeout: Timeout = 5.seconds
    val flushFuture = agoraGuardian ? AgoraGuardianActor.FlushDataCache
    patiently(flushFuture, flushTimeout.duration)
  }
}
