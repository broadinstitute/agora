package org.broadinstitute.dsde.agora.server

import org.scalatest.{BeforeAndAfterAll, Suites}

class AgoraTestSuite extends Suites with BeforeAndAfterAll with AgoraTestFixture {

  val agora = new Agora()


  override def beforeAll() {
    startDatabases()
    println(s"Starting Agora web services ($suiteName)")
    agora.start()
  }


  override def afterAll() {
    stopDatabases()
    println(s"Stopping Agora web services ($suiteName)")
    agora.stop()
  }
}
