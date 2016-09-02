package org.broadinstitute.dsde.agora.server.webservice

import spray.http.StatusCodes
import org.scalatest.DoNotDiscover


@DoNotDiscover
class AgoraServiceStatusSpec extends ApiServiceSpec {

  override def beforeAll() = {
    ensureDatabasesAreRunning()
  }

  override def afterAll() = {
    clearDatabases()
  }

  // our test sql db is H2, which doesn't allow us to check for version
  it should "should not actually be able to test the sql db" in {
    Get(s"/status") ~> apiStatusService.statusRoute ~>
      check {
        assertResult(StatusCodes.InternalServerError) {
          status
        }
        assertResult(s"""{"status": "down", "error": "Function "VERSION" not found; SQL statement:\nselect version(); [90022-175]"}""") {
          responseAs[String]
        }
      }
  }

  ignore should "run and connect to DBs" in {
    Get(s"/status") ~> apiStatusService.statusRoute ~>
      check {
        assertResult(StatusCodes.OK) {
          status
        }
        assertResult(s"""{"status": "up"}""") {
          responseAs[String]
        }
      }
  }
}
