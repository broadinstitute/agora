package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.{ActorRef, ActorRefFactory}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.HealthMonitor.GetCurrentStatus
import org.broadinstitute.dsde.workbench.util.health.StatusCheckResponse

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import org.broadinstitute.dsde.workbench.util.health.StatusJsonSupport._


abstract class StatusService(permissionsDataSource: PermissionsDataSource, healthMonitor: ActorRef) {
  implicit def actorRefFactory: ActorRefFactory
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  
  // Derive timeouts (implicit and not) from akka http's request timeout since there's no point in being higher than that
  implicit val duration = ConfigFactory.load().as[FiniteDuration]("akka.http.server.request-timeout")
  implicit val timeout: Timeout = duration

  // GET /status
  def statusRoute: Route = path("status") {
    get {
      val statusFuture: Future[StatusCheckResponse] = (healthMonitor ? GetCurrentStatus).mapTo[StatusCheckResponse]

      onComplete(statusFuture) { status: Try[StatusCheckResponse] =>
        if (status.get.ok)
          complete {
            HttpResponse.apply(StatusCodes.OK, List[HttpHeader](),
              HttpEntity.apply(ContentTypes.`application/json`, StatusCheckResponseFormat.write(status.get).toString()), HttpProtocols.`HTTP/1.1`)
          }
        else
          complete {
            HttpResponse.apply(StatusCodes.InternalServerError, List[HttpHeader](),
              HttpEntity.apply(ContentTypes.`application/json`, StatusCheckResponseFormat.write(status.get).toString()), HttpProtocols.`HTTP/1.1`)
          }
      }
    }
  }

}
