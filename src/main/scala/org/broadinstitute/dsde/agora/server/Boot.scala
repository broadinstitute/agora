package org.broadinstitute.dsde.agora.server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import net.ceedubs.ficus.Ficus._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Boot extends App with LazyLogging {
  private def startup(): Unit = ???

  startup()
}
