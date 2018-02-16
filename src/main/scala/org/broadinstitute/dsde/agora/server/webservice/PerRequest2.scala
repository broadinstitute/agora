package org.broadinstitute.dsde.agora.server.webservice

import scala.language.postfixOps
import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}

import scala.concurrent.ExecutionContext

object PerRequest2 {
  implicit val requestCompleteMarshaller: ToResponseMarshaller[PerRequestMessage] = Marshaller {
    import scala.concurrent.ExecutionContext.Implicits.global
        
    executionContext: ExecutionContext => {
      case requestComplete: RequestComplete[_] =>
        requestComplete.marshaller(requestComplete.response)
      case requestComplete: RequestCompleteWithHeaders[_] =>
        requestComplete.marshaller(requestComplete.response).map(_.map(_.map(_.mapHeaders(_ ++ requestComplete.headers))))
    }
  }

  sealed trait PerRequestMessage
  /**
    * Report complete, follows same pattern as spray.routing.RequestContext.complete; examples of how to call
    * that method should apply here too. E.g. even though this method has only one parameter, it can be called
    * with 2 where the first is a StatusCode: RequestComplete(StatusCode.Created, response)
    */
  case class RequestComplete[T](response: T)(implicit val marshaller: ToResponseMarshaller[T]) extends PerRequestMessage

  /**
    * Report complete with response headers. To response with a special status code the first parameter can be a
    * tuple where the first element is StatusCode: RequestCompleteWithHeaders((StatusCode.Created, results), header).
    * Note that this is here so that RequestComplete above can behave like spray.routing.RequestContext.complete.
    */
  case class RequestCompleteWithHeaders[T](response: T, headers: HttpHeader*)(implicit val marshaller: ToResponseMarshaller[T]) extends PerRequestMessage

  /**
    * Report complete with a path to use as a Location header
    */
  case class RequestCompleteWithLocation[T](response: T, location: String)(implicit val marshaller: ToResponseMarshaller[T]) extends PerRequestMessage

}
