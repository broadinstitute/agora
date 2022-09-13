package org.broadinstitute.dsde.agora.server

import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

trait AgoraScalaFutures extends ScalaFutures {
  this: AgoraTestFixture =>

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = timeout, interval = 500.milliseconds)
}
