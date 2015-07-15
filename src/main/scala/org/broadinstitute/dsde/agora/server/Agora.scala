package org.broadinstitute.dsde.agora.server

import com.typesafe.scalalogging.slf4j.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AuthorizationProvider
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsAuthorizationProvider

object ProductionAgora extends Agora(GcsAuthorizationProvider) with App {
  override def main(args: Array[String]) {
    start()
  }
}

class Agora(authorizationProvider: AuthorizationProvider) extends LazyLogging with App {
  lazy val server: ServerInitializer = new ServerInitializer()

  sys addShutdownHook stop()

  def start() {
    server.startAllServices(authorizationProvider)
    logger.info("Agora instance " + AgoraConfig.serverInstanceName + " initialized.")
  }

  def stop() {
    logger.info("Stopping server...")
    server.stopAllServices()
    logger.info("Server stopped.")
  }
}

