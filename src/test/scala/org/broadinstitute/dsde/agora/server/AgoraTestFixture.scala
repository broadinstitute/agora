
package org.broadinstitute.dsde.agora.server

import akka.http.scaladsl.model.StatusCodes.OK
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessExecutionContext, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.dataaccess.mongo._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.{BodyWithContentType, HttpRequest, StringBody}
import org.mongodb.scala._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

trait AgoraTestFixture extends LazyLogging {
  implicit lazy val agoraBusinessExecutionContext: AgoraBusinessExecutionContext =
    new AgoraBusinessExecutionContext(scala.concurrent.ExecutionContext.Implicits.global)

  val timeout: FiniteDuration = 10.seconds
  import AgoraConfig.sqlDatabase.profile.api._
  val db: Database = AgoraConfig.sqlDatabase.db
  val permsDataSource: PermissionsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)
  val agoraBusiness: AgoraBusiness = new AgoraBusiness(permsDataSource)
  val permissionBusiness: PermissionBusiness = new PermissionBusiness(permsDataSource)

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

  def addSubstringResponse(payload : String, response : String): Unit = {
    // necessary to declare this here in order to disambiguate overloaded method
    val p2 : BodyWithContentType[String] = StringBody.subString(payload)
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
    TestAgoraMongoClient.ensureRunning()

    clearDatabases()

    logger.debug("Populating sql database.")
    TestPermissionsClient.ensureRunning()
    logger.debug("Finished populating sql database.")
  }

  def stopDatabases(): Unit = {
    clearDatabases()
    logger.debug("Disconnecting from sql database.")
    db.close()
  }

  def ensureMongoDatabaseIsRunning(): Unit = {
    TestAgoraMongoClient.ensureRunning()
  }

  def clearMongoCollections(collections: Seq[MongoCollection[Document]] = AgoraMongoClient.getAllCollections): Unit = {
    logger.debug(s"Clearing mongo collections (${collections.map(_.namespace.getCollectionName).mkString(", ")}).")
    TestAgoraMongoClient.clean(collections)
  }

  def ensureSqlDatabaseIsRunning(): Unit = {
    logger.debug("Populating sql database.")
    TestPermissionsClient.ensureRunning()
  }

  def clearSqlDatabase(): Unit = {
    logger.debug("Clearing sql database.")
    TestPermissionsClient.clean()
  }

  def ensureDatabasesAreRunning(): Unit = {
    ensureMongoDatabaseIsRunning()
    ensureSqlDatabaseIsRunning()
  }

  def clearDatabases(): Unit = {
    clearMongoCollections()
    clearSqlDatabase()
  }

  def addAdminUser(): Int = {
    Await.result(db.run(users += UserDao(adminUser.get, isAdmin = true)), timeout)
  }

  def addUppercaseAdminUser(): Int = {
    Await.result(db.run(users += UserDao(uppercasedAdminUser.get.toUpperCase, isAdmin = true)), timeout)
  }

  protected def patiently[R](op: => Future[R], duration: Duration = 1 minutes): R = {
    Await.result(op, duration)
  }

  protected def runInDB[R](action: DataAccess => DBIOAction[R, _ <: NoStream, _ <: Effect], duration: Duration = 1 minutes): R = {
    patiently(permsDataSource.inTransaction { db => action(db).asInstanceOf[ReadWriteAction[R]] }, duration)
  }
}
