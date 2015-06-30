package org.broadinstitute.dsde.agora.server.webservice.routes

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraEntityProjection, AgoraEntity}
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages.{Add, Query, QuerySingle}
import org.broadinstitute.dsde.agora.server.webservice.PerRequestCreator
import spray.routing.{Directive0, Directives, RequestContext}

import scala.util.Try


trait RouteHelpers extends AgoraDirectives
  with QuerySingleHelper
  with QueryRouteHelper
  with AddRouteHelper

trait BaseRoute extends PerRequestCreator with RouteUtil

trait QuerySingleHelper extends BaseRoute {

  def matchQuerySingleRoute(_path: String) = get & path(_path / Segment / Segment / IntNumber)
  val extractOnlyPayloadParameter = extract(_.request.uri.query.get("onlyPayload"))

  def completeWithPerRequest(context: RequestContext,
                              entity: AgoraEntity,
                              username: String,
                              onlyPayload: Boolean,
                              path: String,
                              queryHandler: Props): Unit = {

    val entityType = AgoraEntityType.byPath(path)
    val message = QuerySingle(context, entity, entityType, username, onlyPayload)

    perRequest(context, queryHandler, message)
  }
}

trait QueryRouteHelper extends BaseRoute {

  // queryRoute Helpers
  def matchQueryRoute(_path: String) = get & path(_path)

  def entityFromParams(params: Map[String, List[String]]): AgoraEntity = {
    val namespace   = params.getOrElse("namespace", Nil).headOption
    val name        = params.getOrElse("name", Nil).headOption
    val _id         = params.getOrElse("snapshotId", Nil).headOption.toIntOption
    val synopsis    = params.getOrElse("synopsis", Nil).headOption
    val docs        = params.getOrElse("documentation", Nil).headOption
    val owner       = params.getOrElse("owner", Nil).headOption
    //    val createDate  = params.getOrElse("createDate", Nil).headOption // cannot search by dateTime yet
    val payload     = params.getOrElse("payload", Nil).headOption
    val url         = params.getOrElse("url", Nil).headOption
    val _type       = params.getOrElse("entityType", Nil).headOption.toAgoraEntityOption

    AgoraEntity(namespace, name, _id, synopsis, docs, owner, createDate = None, payload, url, _type)
  }

  def validateEntityType(params: Map[String, List[String]], path: String): Directive0 = {
    val _type = params.getOrElse("entityType", Nil).headOption.toAgoraEntityOption
    validateEntityType(_type, path)
  }

  def projectionFromParams(params: Map[String, List[String]]): Option[AgoraEntityProjection] = {
    val includeFields = params.getOrElse("includedField", Seq.empty[String])
    val excludeFields = params.getOrElse("excludedField", Seq.empty[String])
    val agoraProjection = AgoraEntityProjection(includeFields, excludeFields)
    agoraProjection.totalFields match {
      case 0 => None
      case _ => Some(agoraProjection)
    }
  }

  def completeWithPerRequest(context: RequestContext,
                             params: Map[String, List[String]],
                             username: String,
                             path: String,
                             queryHandler: Props): Unit = {

    val entity = entityFromParams(params)
    val entityType = AgoraEntityType.byPath(path)
    val projection = projectionFromParams(params)
    val message = Query(context, entity, projection, entityType, username)
    perRequest(context, queryHandler, message)
  }
}

trait AddRouteHelper extends BaseRoute {

  def postPath(_path: String) = post & path(_path)

  def validateEntityType(entity: AgoraEntity, path: String): Directive0 = {
    validateEntityType(entity.entityType, path)
  }

  def completeWithPerRequest(context: RequestContext,
                             entity: AgoraEntity,
                             username: String,
                             addHandler: Props ) = {
    perRequest(context, addHandler, Add(context, entity, username))
  }
}

trait RouteUtil extends Directives {

  def validateEntityType(entityType: Option[AgoraEntityType.EntityType], path: String): Directive0 = {
    val possibleTypes = AgoraEntityType.byPath(path)

    if (entityType.isDefined)
      validate(possibleTypes.contains(entityType.get),
               s"You can't perform operation for entity type $entityType.get at path /$path.")
    else
      pass
  }

  def toBool(x: Option[String]): Boolean = {
    Try(x.get.toBoolean).getOrElse(false)
  }

  // Option converters
  implicit class OptionImplicits(x: Option[String]) {
    def toIntOption: Option[Int] = x match {
      case Some(str) => Try(Option(str.toInt)).getOrElse(None)
      case None => None
    }

    def toAgoraEntityOption: Option[AgoraEntityType.EntityType] = x match {
      case Some(str) => Try(Option(AgoraEntityType.withName(str.capitalize))).getOrElse(None)
      case None => None
    }
  }

}
