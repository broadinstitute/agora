package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.Actor
import cromwell.binding.{WdlSource, WdlNamespace}
import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType, AgoraError}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.RequestComplete
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages
import org.joda.time.DateTime
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

/**
 * Handles adding a method to the methods repository, including validation.
 */
class AddHandler(authorizationProvider: AuthorizationProvider) extends Actor {
  implicit val system = context.system

  val agoraBusiness = new AgoraBusiness()

  def receive = {
    case ServiceMessages.Add(requestContext: RequestContext, agoraAddRequest: AgoraEntity, username: String) =>
      try {
        validatePayload(agoraAddRequest, username)
        add(requestContext, agoraAddRequest, username)
      } catch {
        case e: SyntaxError => context.parent ! RequestComplete(BadRequest, AgoraError("Syntax error in payload: " + e.getMessage))
      }
      context.stop(self)
  }

  private def validatePayload(agoraEntity: AgoraEntity, username: String): Unit = {
    agoraEntity.entityType.get match {
      case AgoraEntityType.Task =>
        WdlNamespace.load(agoraEntity.payload.get)
      case AgoraEntityType.Workflow =>
        val resolver = MethodImportResolver(username, agoraBusiness, authorizationProvider)
        WdlNamespace.load(agoraEntity.payload.get, resolver.importResolver _)
      case AgoraEntityType.Configuration =>
      //add config validation here
    }
  }

  private def add(requestContext: RequestContext, agoraEntity: AgoraEntity, username: String): Unit = {
    if(authorizationProvider.isAuthorizedForCreation(agoraEntity, username)) {
      val method = agoraBusiness.insert(agoraEntity.copy(createDate = Option(new DateTime())), username)
      authorizationProvider.createEntityAuthorizations(method, username)
      context.parent ! RequestComplete(Created, method)
    }
    else {
      context.parent ! RequestComplete(BadRequest, AgoraError("You don't have permission to create items in " + agoraEntity.namespace))
    }
  }

}

case class MethodImportResolver(username: String, business: AgoraBusiness, authorizationProvider: AuthorizationProvider) {

  def importResolver(importUri: String): WdlSource = {
    ImportResolverHelper.validateUri(importUri)
    val method = ImportResolverHelper.resolve(importUri, business, username)
    if (method == None) throw new SyntaxError(s"Can't resolve import: $importUri")
    val canRead = authorizationProvider.isAuthorizedForRead(method.get, username)
    if (canRead) method.get.payload.get
    else throw new SyntaxError(s"Can't resolve import: $importUri")
  }
}

object ImportResolverHelper {

  private val methodScheme = "methods://"

  def resolve(uri: String, business: AgoraBusiness, username: String): Option[AgoraEntity] = {
    val withoutScheme = removeScheme(uri)
    val parts = uriParts(withoutScheme)
    val namespace = parts(0)
    val name = parts(1)
    val snapshotId = parts(2).toInt
    val queryMethod = AgoraEntity(namespace = Option(namespace), name = Option(name), snapshotId = Option(snapshotId))
    val method = business.findSingle(queryMethod.namespace.get,
                                    queryMethod.name.get,
                                    queryMethod.snapshotId.get,
                                    Seq(AgoraEntityType.Task, AgoraEntityType.Workflow),
                                    username)
    method
  }

  def validateUri(importUri: String) = {
    if (!importUri.startsWith(methodScheme)) {
      throw new SyntaxError(s"Unsupported import uri: $importUri. Must start with $methodScheme.")
    }
    val withoutScheme = removeScheme(importUri)
    val parts = uriParts(withoutScheme)
    if (parts.size != 3) {
      throw new SyntaxError(s"Malformed methods uri. Must contain three parts, separated by '.': $importUri")
    }
    if (!isIdInt(parts(2))) {
      throw new SyntaxError(s"Malformed methods uri. SnapshotId must be integer: $importUri")
    }
  }

  private def removeScheme(uri: String): String = {
    uri.substring(methodScheme.length)
  }

  private def isIdInt(idStr: String): Boolean = {
    idStr.forall(_.isDigit)
  }

  private def uriParts(uriWithoutScheme: String): Array[String] = {
    uriWithoutScheme.split("\\.")
  }
}
