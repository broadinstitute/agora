
package org.broadinstitute.dsde.agora.server

import akka.http.scaladsl.model.StatusCodes.OK
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.MongoDBObject
import org.broadinstitute.dsde.agora.server.AgoraTestData.{waasMockServerPort, _}
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.{AgoraMongoClient, EmbeddedMongo}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.{Body, BodyWithContentType, StringBody}
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

trait AgoraTestFixture {
  import scala.concurrent.ExecutionContext.Implicits.global

  val timeout = 10.seconds
  val db: Database = AgoraConfig.sqlDatabase.db
  val permsDataSource: PermissionsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)
  val agoraBusiness: AgoraBusiness = new AgoraBusiness(permsDataSource)
  val permissionBusiness: PermissionBusiness = new PermissionBusiness(permsDataSource)

  var waasMockServer: ClientAndServer = _
  val waasRequest = request()
    .withMethod("POST")
    .withPath("/api/womtool/v1/describe")

  val mockAccessToken = AgoraConfig.mockAccessToken

  def startMockWaas() = {
    waasMockServer = startClientAndServer(waasMockServerPort)
    addSubstringResponse(payload1.get, payload1DescribeResponse)
    addSubstringResponse(payload2.get, payload2DescribeResponse)
    addSubstringResponse(payloadWithValidPersonalDockerImageInWdl.get, genericDockerPayloadDescribeResponse)
    addSubstringResponse(payloadWithValidOfficialDockerImageInWdl.get, genericDockerPayloadDescribeResponse)
    addSubstringResponse(payloadWdlBadVersion.get, badVersionDescribeResponse)
    addSubstringResponse(badPayload.get, malformedPayloadDescribeResponse)
    addSubstringResponse(payloadWdl10.get, goodWdlVersionDescribeResponse)
  }

  def addSubstringResponse(payload : String, response : String) = {
    // necessary to declare this here in order to disambiguate overloaded method
    val p2 : BodyWithContentType[String] = new StringBody(payload, true)
    waasMockServer.when(
      waasRequest
        .withBody(p2)
        )
      .respond(
        org.mockserver.model.HttpResponse.response().withBody(response).withStatusCode(OK.intValue).withHeader("Content-Type", "application/json"))

  }

  def stopMockWaas() = {
    waasMockServer.stop()
  }

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
