package org.broadinstitute.dsde.agora.server

import com.typesafe.scalalogging.slf4j.LazyLogging

object Agora extends LazyLogging {
  val server: ServerInitializer = {
    new ServerInitializer()
  }

  sys addShutdownHook stop()

  def main(args: Array[String]) {
    start()
  }

  def start(): Unit = {
    server.startAllServices()
    logger.info("Agora instance " + AgoraConfig.serverInstanceName + " initialized.")
  }

  def stop() {
    logger.info("Stopping server...")
    server.stopAllServices()
    logger.info("Server stopped.")
  }
}

