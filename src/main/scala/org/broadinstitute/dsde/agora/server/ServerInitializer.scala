package org.broadinstitute.dsde.agora.server

import akka.actor.ActorSystem
import akka.io.IO
import akka.io.Tcp.CommandFailed
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.EmbeddedMongo
import org.broadinstitute.dsde.agora.server.webservice.ApiServiceActor
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import spray.can.Http

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class ServerInitializer extends LazyLogging {
  implicit val actorSystem = ActorSystem("agora")
  val permsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)

  def startAllServices() {
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.startMongo()

    startWebServiceActors()
  }

  def stopAllServices() {
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.stopMongo()

    println("Closing connection to sql db.")
    permsDataSource.close()

    stopAndCatchExceptions(stopWebServiceActors())
  }

  private def startWebServiceActors() = {
    implicit val bindTimeout: Timeout = 120.seconds
    val service = actorSystem.actorOf(ApiServiceActor.props(permsDataSource), "agora-actor")
    Await.result(IO(Http) ? Http.Bind(service, interface = AgoraConfig.webserviceInterface, port = AgoraConfig.port), bindTimeout.duration) match {
      case CommandFailed(b: Http.Bind) =>
        logger.error(s"Unable to bind to port ${AgoraConfig.port} on interface ${AgoraConfig.webserviceInterface}")
        actorSystem.shutdown()
        stopAndExit()
      case _ => logger.info("Actor system started.")
    }
  }

  private def stopWebServiceActors() {
    IO(Http) ! Http.CloseAll
  }

  private def stopAndExit() {
    logger.info("Stopping all services and exiting.")
    stopAllServices()
    logger.info("Services stopped")
    throw new RuntimeException("Errors were found while initializing Agora.  This server will shutdown.")
  }

  private def stopAndCatchExceptions(closure: => Unit) {
    Try(closure).recover {
      case ex: Throwable => logger.error("Exception ignored while shutting down.", ex)
    }
  }
}
