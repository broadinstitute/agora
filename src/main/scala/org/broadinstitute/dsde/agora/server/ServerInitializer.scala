package org.broadinstitute.dsde.agora.server

import akka.actor.CoordinatedShutdown
import akka.actor.typed._
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RoutingLog
import akka.{actor => classic}
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.business.AgoraBusinessExecutionContext
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.agora.server.dataaccess.health.AgoraHealthMonitorSubsystems._
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.EmbeddedMongo
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.ApiService

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

class ServerInitializer extends LazyLogging {
  private val permsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)

  private val dbStatus = new AgoraDBStatus(permsDataSource)
  private implicit val actorSystem: ActorSystem[AgoraGuardianActor.Command] =
    ActorSystem(AgoraGuardianActor(permsDataSource, dbStatus.toHealthMonitorSubsystems), "agora")

  private val ioDispatcherName = "dispatchers.io-dispatcher"
  private val ioDispatcher = actorSystem.dispatchers.lookup(DispatcherSelector.fromConfig(ioDispatcherName))

  // DANGER: Only pass this execution context explicitly! If the actor system's execution context accidentally ends up
  // being used implicitly in the wrong place it can cause timeouts that currently only appear in production.
  // https://github.com/broadinstitute/agora/pull/297
  private val executionContext: ExecutionContext = actorSystem.executionContext
  private val agoraBusinessExecutionContext = new AgoraBusinessExecutionContext(ioDispatcher)

  // Initialize the CoordinatedShutdown now so that all Akka hooks are installed
  CoordinatedShutdown(actorSystem.toClassic)

  def startAllServices(): Unit = {
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.startMongo()
    startWebService()
  }

  def stopAllServices(): Unit = {
    logger.info("Closing all connections")
    Http()(actorSystem.toClassic).shutdownAllConnectionPools()
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.stopMongo()
    permsDataSource.close()
    actorSystem.terminate()
    Await.result(actorSystem.whenTerminated, Duration.Inf)
  }

  private def startWebService() = {

    /*
    These two implicit definitions are required so that scalac does not get confused with Akka Http.
    Otherwise, as of Akka 2.6.1 and Akka Http 10.1.10, multiple implicits handlers are discovered, and none are used.

    For more info on Akka Typed and Akka Classic coexistence, see these examples:
    - https://doc.akka.io/docs/akka/current/typed/coexisting.html
    - https://doc.akka.io/docs/akka/current/typed/from-classic.html
     */
    implicit val classicActorRefFactory: classic.ActorRefFactory = actorSystem.toClassic
    implicit val routingLog: RoutingLog = RoutingLog.fromActorSystem(actorSystem.toClassic)
    val apiService = new ApiService(permsDataSource, actorSystem)(
      executionContext,
      implicitly,
      agoraBusinessExecutionContext
    )
    Http()(actorSystem.toClassic)
      .bindAndHandle(apiService.route, AgoraConfig.webserviceInterface, AgoraConfig.port)
      .recover {
        case t: Throwable =>
          logger.error(s"Unable to bind to port ${AgoraConfig.port} on interface ${AgoraConfig.webserviceInterface}")
          stopAndExit()
          throw t
      }(executionContext)
  }

  private def stopAndExit(): Unit = {
    logger.info("Stopping all services and exiting.")
    stopAllServices()
    logger.info("Services stopped")
    throw new RuntimeException("Errors were found while initializing Agora.  This server will shutdown.")
  }
}
