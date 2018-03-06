package org.broadinstitute.dsde.agora.server.webservice.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer

import scala.concurrent.Future

trait HttpClient {

  implicit val actorSystem = ActorSystem("agoraHttpClient")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  def get(uri: Uri): Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uri, method = GET))
  
}
