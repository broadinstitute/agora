package org.broadinstitute.dsde.agora.server.webservice.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {

  implicit def actorSystem: ActorSystem
  implicit def materializer: ActorMaterializer
  implicit def executionContext: ExecutionContext = actorSystem.dispatcher

  def get(uri: Uri): Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = uri, method = GET))
  
}
