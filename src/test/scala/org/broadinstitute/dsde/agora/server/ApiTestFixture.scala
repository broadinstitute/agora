package org.broadinstitute.dsde.agora.server

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import org.broadinstitute.dsde.agora.server.webservice.ApiServiceActor
import org.scalatest.{BeforeAndAfter, FlatSpec}
import spray.client.pipelining._
import spray.http.StatusCodes._
import spray.http._

import scala.concurrent.{Await, Future}

class ApiTestFixture extends FlatSpec with BeforeAndAfter {
  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5, TimeUnit.SECONDS)

  import system.dispatcher

  before {
    Agora.start()
  }

  after {
    Agora.stop()
  }

  def fixture =
    new {
      val AgoraWebService: HttpRequest => Future[HttpResponse] = sendReceive(system.actorOf(ApiServiceActor.props))
    }

  "Agora" should "be started" in {
    assert(pingServer)
  }

  /**
   * Pings the default route ("/") and checks for an OK response (5 second timeout)
   * @return true if the server responds with OK, false if there is a timeout or the server responds with an error code
   */
  def pingServer: Boolean = {
    Await.result(fixture.AgoraWebService(Get("/")), timeout.duration).status == OK
  }

}