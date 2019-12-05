package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import org.broadinstitute.dsde.agora.server.AgoraGuardianActor
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.workbench.util.health.{StatusCheckResponse, SubsystemStatus, Subsystems}

import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class StatusService(permissionsDataSource: PermissionsDataSource,
                    agoraGuardian: ActorRef[AgoraGuardianActor.Command]) {
  // Derive timeouts (implicit and not) from akka http's request timeout since there's no point in being higher than that
  private val duration: FiniteDuration = ConfigFactory.load().as[FiniteDuration]("akka.http.server.request-timeout")

  // GET /status
  def statusRoute: Route = path("status") {
    get {
      extractActorSystem { implicit actorSystem =>
        implicit val timeout: Timeout = duration
        implicit val scheduler: Scheduler = actorSystem.toTyped.scheduler

        val statusAttempt = agoraGuardian ? AgoraGuardianActor.GetCurrentStatus

        import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
        import org.broadinstitute.dsde.workbench.util.health.StatusJsonSupport._

        onComplete(statusAttempt) {
          case Success(status) =>
            val httpCode = if (status.ok) OK else InternalServerError
            complete(httpCode, status)
          case Failure(NonFatal(nonFatal)) =>
            val agoraStatus =
              SubsystemStatus(ok = false, Option(List(s"Unable to gather subsystem status: ${nonFatal.getMessage}")))
            complete(InternalServerError, StatusCheckResponse(ok = false, Map(Subsystems.Agora -> agoraStatus)))
          case Failure(throwable) => throw throwable
        }
      }
    }
  }
}
