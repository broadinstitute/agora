package org.broadinstitute.dsde.agora.server.webservice

import org.broadinstitute.dsde.agora.server.model.AgoraStatus
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
        assert(responseAs[String] != null)
      }
  }

  ignore should "run and connect to DBs" in {
    Get(s"/status") ~> apiStatusService.statusRoute ~>
      check {
        assertResult(StatusCodes.OK) {
          status
        }
        assert(responseAs[String] != null)
      }
  }
}
