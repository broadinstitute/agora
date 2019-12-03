package org.broadinstitute.dsde.agora.server

import akka.actor.{ActorRef, ActorSystem, Cancellable}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.business.AgoraBusinessExecutionContext
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDBStatus
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.EmbeddedMongo
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AdminSweeper.Sweep
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AdminSweeper, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.webservice.ApiService
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor
import org.broadinstitute.dsde.workbench.util.health.Subsystems.{Database, Mongo}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try

class ServerInitializer extends LazyLogging {
  implicit val actorSystem: ActorSystem = ActorSystem("agora")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = actorSystem.dispatcher

  private val ioDispatcherName = "dispatchers.io-dispatcher"
  private val healthMonitorDispatcherName = "dispatchers.health-monitor-dispatcher"
  private val adminSweeperDispatcherName = "dispatchers.admin-sweeper-dispatcher"
  private val ioDispatcher = actorSystem.dispatchers.lookup(ioDispatcherName)
  private val healthMonitorDispatcher = actorSystem.dispatchers.lookup(healthMonitorDispatcherName)
  private val adminSweeperDispatcher = actorSystem.dispatchers.lookup(adminSweeperDispatcherName)

  private implicit val agoraBusinessExecutionContext: AgoraBusinessExecutionContext =
    new AgoraBusinessExecutionContext(ioDispatcher)
  val permsDataSource = new PermissionsDataSource(AgoraConfig.sqlDatabase)


  // create these outside of the startAllServices def, so we have global access to their vals
  private val dbStatus = new AgoraDBStatus(permsDataSource)
  val healthMonitor: ActorRef = actorSystem.actorOf(HealthMonitor.props(Set(Database, Mongo))({ () =>
    Map(
      Database -> dbStatus.mysqlStatus()(healthMonitorDispatcher),
      Mongo -> dbStatus.mongoStatus()(healthMonitorDispatcher)
    )
  }).withDispatcher(healthMonitorDispatcherName), "health-monitor")
  private var healthMonitorSchedule: Cancellable = _
  private var adminGroupPollerSchedule: Cancellable = _

  def startAllServices(): Unit = {
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.startMongo()

    healthMonitorSchedule = actorSystem.scheduler.schedule(3.seconds, 1.minute, healthMonitor, HealthMonitor.CheckAll)(
      executor = healthMonitorDispatcher
    )

    startAdminGroupPoller()
    startWebService()
  }

  def stopAllServices(): Unit = {
    logger.info("Closing all connections")
    Http().shutdownAllConnectionPools()
    if (AgoraConfig.usesEmbeddedMongo)
      EmbeddedMongo.stopMongo()
    permsDataSource.close()
    healthMonitorSchedule.cancel() // stop the health monitor
    stopAdminGroupPoller() // stop the admin google group poller
  }

  private def startWebService() = {

    val apiService = new ApiService(permsDataSource, healthMonitor)

    Http().bindAndHandle(apiService.route, AgoraConfig.webserviceInterface, AgoraConfig.port)
      .recover {
        case t: Throwable =>
          logger.error(s"Unable to bind to port ${AgoraConfig.port} on interface ${AgoraConfig.webserviceInterface}")
          actorSystem.terminate()
          stopAndExit()
          throw t
      }
  }

  /**
    * Firecloud system maintains its set of admins as a google group.
    * If such a group is specified in config, poll it at regular intervals
    * to synchronize the admins defined in our users table.
    */
  private def startAdminGroupPoller(): Unit = {
    AgoraConfig.adminGoogleGroup match {
      case Some(_) =>
        val adminGroupPoller = actorSystem.actorOf(
          AdminSweeper
            .props(AdminSweeper.adminsGoogleGroupPoller, permsDataSource)
            .withDispatcher(adminSweeperDispatcherName)
        )
        adminGroupPollerSchedule = actorSystem.scheduler
          .schedule(5 seconds, AgoraConfig.adminSweepInterval minutes, adminGroupPoller, Sweep)(
            executor = adminSweeperDispatcher
          )
      case None =>
    }
  }

  private def stopAdminGroupPoller(): Try[Boolean] = {
    Try(adminGroupPollerSchedule.cancel()) recover {
      case _: NullPointerException =>
        false // Nothing to do; no scheduler was created at the first place
      case t: Throwable =>
        logger.warn(s"Unable to stop the admin group poller because '${t.getMessage}'")
        false
    }
  }

  private def stopAndExit(): Unit = {
    logger.info("Stopping all services and exiting.")
    stopAllServices()
    logger.info("Services stopped")
    throw new RuntimeException("Errors were found while initializing Agora.  This server will shutdown.")
  }
}
