package org.broadinstitute.dsde.agora.server

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.{actor => classic}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterAll, Ignore, Suite}

import scala.concurrent.ExecutionContextExecutor

/**
 * A mixin for ScalaTest suites that spins up and shuts down an actor test kit.
 */
trait AgoraAkkaTest extends BeforeAndAfterAll {
  this: Suite =>

  // Begin borrowed from akka.http.scaladsl.testkit.RouteTest

  def actorSystemNameFrom(clazz: Class[_]): String =
    clazz.getName
      .replace('.', '-')
      .replace('_', '-')
      .filter(_ != '$')

  // Despite ScalaTest warnings, this is not meant to be a test.
  // But we're keeping the name consistent with the ScalatestRouteTest def.
  @Ignore // <-- warning still gets printed
  def testConfigSource: String = ""

  // Despite ScalaTest warnings, this is not meant to be a test.
  // But we're keeping the name consistent with the ScalatestRouteTest def.
  @Ignore // <-- warning still gets printed
  def testConfig: Config = {
    val source = testConfigSource
    val config = if (source.isEmpty) ConfigFactory.empty() else ConfigFactory.parseString(source)
    config.withFallback(ConfigFactory.load())
  }

  // End borrowed from akka.http.scaladsl.testkit.RouteTest

  // NOTE: val's must be lazy! https://docs.scala-lang.org/tutorials/FAQ/initialization-order.html
  protected lazy val actorTestKit: ActorTestKit = ActorTestKit(actorSystemNameFrom(getClass), testConfig)

  implicit lazy val systemTyped: ActorSystem[Nothing] = actorTestKit.system

  protected def createActorSystem(): classic.ActorSystem = {
    systemTyped.toClassic
  }

  implicit def executor: ExecutionContextExecutor = createActorSystem().dispatcher

  def cleanUp(): Unit = actorTestKit.shutdownTestKit()

  abstract override protected def afterAll(): Unit = {
    cleanUp()
    super.afterAll()
  }
}
