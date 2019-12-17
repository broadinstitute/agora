package org.broadinstitute.dsde.agora.server.actor

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

/**
 * Reads data using a provided function and returns it when asked.
 */
object ReadCacheActor {

  /**
   * Parameters to create an instance of a Read Cache Actor.
   *
   * @param description   A description of the instance of the actor.
   * @param cacheDuration The amount of time between requests to the doRefresh function.
   * @param doRefresh     The function that retrieves the data to be cached.
   * @tparam A The type of data to be cached.
   */
  case class Params[A](description: String, cacheDuration: FiniteDuration, doRefresh: () => Future[A])

  //@formatter:off
  sealed trait Command
  final case class Request[A](replyTo: ActorRef[A]) extends Command
  final case class Flush(replyTo: ActorRef[Flushed.type]) extends Command
  private final case object RefreshCache extends Command
  private final case class Refreshed[A](reply: A) extends Command
  private final case object RefreshFailed extends Command

  private final case object RefreshSingleTimer
  private final case object RefreshDelayTimer

  final case object Flushed
  //@formatter:on

  /**
   * Internal state of the cache.
   *
   * @param params          Parameters for this cache.
   * @param listReplyOption The current contents of the cache, None when flushed, Some() when full.
   * @param replyTos        The list of ActorRef instances awaiting a reply.
   * @tparam A The type of data to return.
   */
  private final case class State[A](params: Params[A],
                                    listReplyOption: Option[A],
                                    replyTos: Set[ActorRef[A]])

}

class ReadCacheActor[Response] {

  import ReadCacheActor._

  def create(params: Params[Response]): Behavior[Command] = {
    Behaviors.setup { _ =>
      Behaviors.withTimers { timers =>
        timers.startSingleTimer(RefreshSingleTimer, RefreshCache, 3.seconds)
        timers.startTimerWithFixedDelay(RefreshDelayTimer, RefreshCache, params.cacheDuration)
        run(State(params, None, Set.empty))
      }
    }
  }

  private def run(state: State[Response]): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      val log = context.log
      val self = context.self
      val executionContext = context.executionContext

      message match {
        case Flush(replyTo: ActorRef[Flushed.type]) =>
          self ! RefreshCache
          replyTo ! Flushed
          val newState = state.copy(listReplyOption = None)
          run(newState)

        case Request(replyTo: ActorRef[Response]) =>
          state.listReplyOption match {
            case Some(listReply) =>
              replyTo ! listReply
              Behaviors.same
            case None =>
              val newState = state.copy(replyTos = state.replyTos + replyTo)
              run(newState)
          }

        case RefreshCache =>
          state.params.doRefresh().onComplete {
            case Success(reply) =>
              log.debug(s"Refreshed ${state.params.description}")
              self ! Refreshed(reply)
            case Failure(NonFatal(throwable)) =>
              log.error(s"Refreshing ${state.params.description} failed: ${throwable.getMessage}", throwable)
              self ! RefreshFailed
            case Failure(throwable) => throw throwable
          }(executionContext)
          Behaviors.same

        case RefreshFailed =>
          val newState = state.copy(listReplyOption = None)
          run(newState)

        case Refreshed(reply: Response@unchecked) =>
          state.replyTos.foreach(_ ! reply)
          val newState: State[Response] = state.copy(listReplyOption = Option(reply), replyTos = Set.empty)
          run(newState)
      }
    }
  }
}
