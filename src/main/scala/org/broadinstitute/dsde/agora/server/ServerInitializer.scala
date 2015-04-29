package org.broadinstitute.dsde.agora.server

import akka.actor.ActorSystem
import akka.io.IO
import akka.io.Tcp.CommandFailed
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.Config
import com.typesafe.scalalogging.slf4j.LazyLogging
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.webservice.ApiServiceActor
import spray.can.Http
import scala.concurrent.duration._
import scala.concurrent.Await

import scala.util.Try

class ServerInitializer(val config: Config) extends LazyLogging {
  implicit val actorSystem = ActorSystem("agora")

  lazy val serverInstanceName = config.as[String]("instance.name")
  lazy val webservicePort = config.as[Option[Int]]("webservice.port").getOrElse(8000)
  lazy val webserviceInterface = config.as[Option[String]]("webservice.interface").getOrElse("0.0.0.0")

  def startAllServices() {
    startWebServiceActors()
  }

  def stopAllServices() {
    stopAndCatchExceptions(stopWebServiceActors())
  }

  private def startWebServiceActors() = {
    implicit val timeout = Timeout(5.seconds)
    val service = actorSystem.actorOf(ApiServiceActor.props, "agora-actor")
    Await.result(IO(Http) ? Http.Bind(service, interface = webserviceInterface, port = webservicePort), timeout.duration) match {
      case CommandFailed(b: Http.Bind) =>
        logger.error(s"Unable to bind to port $webservicePort on interface $webserviceInterface")
        actorSystem.shutdown()
        stopAndExit()
      case _ =>
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
