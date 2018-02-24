package org.broadinstitute.dsde.agora.server

import akka.actor.{ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.EmbeddedMongo
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.webservice.ApiService
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor
import org.broadinstitute.dsde.workbench.util.health.Subsystems.{Database, Mongo}

import scala.concurrent.duration._
import scala.util.Try

class ServerInitializer2 extends LazyLogging {
  implicit val actorSystem = ActorSystem("agora")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  val permsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)


  // create these outside of the startAllServices def, so we have global access to their vals
  private val dbStatus = new AgoraDBStatus(permsDataSource)
  val healthMonitor = actorSystem.actorOf(HealthMonitor.props(Set(Database, Mongo)) { () =>
    Map(Database -> dbStatus.mysqlStatus, Mongo -> dbStatus.mongoStatus)
  }, "health-monitor")
  private var healthMonitorSchedule: Cancellable = _

  def startAllServices() {
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.startMongo()

    healthMonitorSchedule = actorSystem.scheduler.schedule(3.seconds, 1.minute, healthMonitor, HealthMonitor.CheckAll)

    startWebServiceActors()
  }

  def stopAllServices() {
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.stopMongo()

    println("Closing connection to sql db.")
    permsDataSource.close()

    healthMonitorSchedule.cancel() // stop the health monitor

    //stopAndCatchExceptions(stopWebServiceActors())
  }

  private def startWebServiceActors() = {
    implicit val bindTimeout: Timeout = 120.seconds

    val apiService = new ApiService(permsDataSource, healthMonitor)

    // TODO Verify that the port is 8000 and not 8080
    Http().bindAndHandle(apiService.route, "0.0.0.0", 8000)
      .recover {
        case t: Throwable =>
          logger.error(s"Unable to bind to port ${AgoraConfig.port} on interface ${AgoraConfig.webserviceInterface}")
          actorSystem.terminate()
          stopAndExit()
          throw t
      }
  }

//  private def stopWebServiceActors() {
//    IO(Http) ! Http.CloseAll
//  }

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
