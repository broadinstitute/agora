package org.broadinstitute.dsde.agora.server

import akka.{actor => classic}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.Config
import org.scalatest.Suite

import scala.concurrent.ExecutionContextExecutor

/**
 * Extension of AgoraAkkaTest plus ScalatestRouteTest, and implements the overrides such that the compiler knows to use
 * the AgoraAkkaTest version of duplicated methods.
 */
trait AgoraRouteTest extends AgoraAkkaTest with ScalatestRouteTest {
  this: Suite =>

  abstract override def actorSystemNameFrom(clazz: Class[_]): String = {
    super[AgoraAkkaTest].actorSystemNameFrom(clazz)
  }

  abstract override def testConfigSource: String = {
    super[AgoraAkkaTest].testConfigSource
  }

  abstract override def testConfig: Config = {
    super[AgoraAkkaTest].testConfig
  }

  abstract override protected def createActorSystem(): classic.ActorSystem = {
    super[AgoraAkkaTest].createActorSystem()
  }

  implicit abstract override def executor: ExecutionContextExecutor = {
    super[AgoraAkkaTest].executor
  }

  abstract override def cleanUp(): Unit = {
    super[AgoraAkkaTest].cleanUp()
  }
}
