package org.broadinstitute.dsde.agora.server

import com.typesafe.scalalogging.slf4j.LazyLogging

object EnvironmentSpecificAgora extends Agora(AgoraConfig.environment) with App {
    start()
}

class Agora(environment: String) extends LazyLogging with App {
  lazy val server: ServerInitializer = new ServerInitializer(environment)

  sys addShutdownHook stop()

  def start() {
    server.startAllServices()
    logger.info("Agora instance " + AgoraConfig.serverInstanceName + " initialized, Environment: " + environment)
  }

  def stop() {
    logger.info("Stopping server...")
    server.stopAllServices()
    logger.info("Server stopped.")
  }
}

