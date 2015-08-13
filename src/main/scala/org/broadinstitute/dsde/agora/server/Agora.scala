package org.broadinstitute.dsde.agora.server

import com.typesafe.scalalogging.slf4j.LazyLogging

object ProductionAgora extends Agora {
    start()
}

class Agora extends LazyLogging with App {
  lazy val server: ServerInitializer = new ServerInitializer()

  sys addShutdownHook stop()

  def start() {
    server.startAllServices()
    logger.info("Agora instance " + AgoraConfig.serverInstanceName + " initialized, Environment: " + AgoraConfig.environment)
  }

  def stop() {
    logger.info("Stopping server...")
    server.stopAllServices()
    logger.info("Server stopped.")
  }
}

