package org.broadinstitute.dsde.agora.server.webservice.routes

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.AgoraConfig.{authenticationDirectives, version}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AccessControl
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.PerRequestCreator
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages._
import shapeless.HList
import spray.routing._

import scala.util.Try

trait RouteHelpers extends QuerySingleHelper
  with QueryRouteHelper
  with AddRouteHelper
  with EntityPermissionsRouteHelper
  with NamespacePermissionsRouteHelper

trait BaseRoute extends PerRequestCreator with RouteUtil  {
  def getUserFromParams(params: Map[String, String]): String = {
    val user = params.get("user")
    if (user.isDefined)
      user.get
    else
      throw new IllegalArgumentException("Missing params: user and/or role.")
  }

  def versionedPath[L <: HList](pm: PathMatcher[L]): Directive[L] = path("api" / version / pm)
}

trait NamespacePermissionsRouteHelper extends BaseRoute {

  def matchNamespacePermissionsRoute(_path: String) =
    versionedPath(_path / Segment / "permissions") &
    authenticationDirectives.usernameFromRequest()

  def completeNamespacePermissionsGet(context: RequestContext, entity: AgoraEntity, username: String, permissionsHandler: Props) = {
    val message = ListNamespacePermissions(context, entity, username)
    perRequest(context, permissionsHandler, message)
  }

  def completeBatchNamespacePermissionsPost(context: RequestContext, entity: AgoraEntity, accessObjects: List[AccessControl], username: String, permissionHandler: Props) = {
    val message = BatchNamespacePermission(context, entity, username, accessObjects)
    perRequest(context, permissionHandler, message)
  }

  def completeNamespacePermissionsPost(context: RequestContext, entity: AgoraEntity, params: Map[String, String], username: String, permissionsHandler: Props) ={
    val accessObject = AccessControl.fromParams(params)
    val message = InsertNamespacePermission(context, entity, username, accessObject)
    perRequest(context, permissionsHandler, message)
  }

  def completeNamespacePermissionsPut(context: RequestContext, entity: AgoraEntity, params: Map[String, String], username: String, permissionsHandler: Props) ={
    val accessObject = AccessControl.fromParams(params)
    val message = EditNamespacePermission(context, entity, username, accessObject)
    perRequest(context, permissionsHandler, message)
  }

  def completeNamespacePermissionsDelete(context: RequestContext, entity: AgoraEntity, params: Map[String, String], username: String, permissionsHandler: Props) = {
    val userToRemove = getUserFromParams(params)
    val message = DeleteNamespacePermission(context, entity, username, userToRemove)
    perRequest(context, permissionsHandler, message)
  }
}

trait EntityPermissionsRouteHelper extends BaseRoute {

  def matchEntityPermissionsRoute(_path: String) =
    versionedPath(_path / Segment / Segment / IntNumber / "permissions") &
    authenticationDirectives.usernameFromRequest()

  def completeEntityPermissionsGet(context: RequestContext, entity: AgoraEntity, username: String, permissionsHandler: Props) = {
    val message = ListEntityPermissions(context, entity, username)
    perRequest(context, permissionsHandler, message)
  }

  def completeBatchEntityPermissionsPost(context: RequestContext, entity: AgoraEntity, accessObjects: List[AccessControl], username: String, permissionHandler: Props) = {
    val message = BatchEntityPermission(context, entity, username, accessObjects)
    perRequest(context, permissionHandler, message)
  }

  def completeEntityPermissionsPost(context: RequestContext, entity: AgoraEntity, params: Map[String, String], username: String, permissionsHandler: Props) = {
    val accessObject = AccessControl.fromParams(params)
    val message = InsertEntityPermission(context, entity, username, accessObject)
    perRequest(context, permissionsHandler, message)
  }

  def completeEntityPermissionsPut(context: RequestContext, entity: AgoraEntity, params: Map[String, String], username: String, permissionsHandler: Props) = {
    val accessObject = AccessControl.fromParams(params)
    val message = EditEntityPermission(context, entity, username, accessObject)
    perRequest(context, permissionsHandler, message)
  }

  def completeEntityPermissionsDelete(context: RequestContext, entity: AgoraEntity, params: Map[String, String], username: String, permissionsHandler: Props) = {
    val userToRemove = getUserFromParams(params)
    val message = DeleteEntityPermission(context, entity, username, userToRemove)
    perRequest(context, permissionsHandler, message)
  }
}

trait QuerySingleHelper extends BaseRoute {

  def matchQuerySingleRoute(_path: String) =
    versionedPath(_path / Segment / Segment / IntNumber) &
    authenticationDirectives.usernameFromRequest()

  def extractOnlyPayloadParameter = extract(_.request.uri.query.get("onlyPayload"))

  def completeWithPerRequest(context: RequestContext,
                              entity: AgoraEntity,
                              username: String,
                              onlyPayload: Boolean,
                              path: String,
                              queryHandler: Props): Unit = {
    addUserIfNotInDatabase(username)
    val entityTypes = AgoraEntityType.byPath(path)
    val message = QuerySingle(context, entity, entityTypes, username, onlyPayload)

    perRequest(context, queryHandler, message)
  }

  def completeEntityDelete(context: RequestContext,
                            entity: AgoraEntity,
                            username: String,
                            path: String,
                            queryHandler: Props): Unit = {
    addUserIfNotInDatabase(username)
    val entityTypes = AgoraEntityType.byPath(path)
    val message = Delete(context, entity, entityTypes, username)

    perRequest(context, queryHandler, message)
  }

}

trait QueryRouteHelper extends BaseRoute {

  def matchQueryRoute(_path: String) =
    get &
    versionedPath(_path) &
    authenticationDirectives.usernameFromRequest()

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
    addUserIfNotInDatabase(username)
    val entity = entityFromParams(params)
    val entityType = AgoraEntityType.byPath(path)
    val projection = projectionFromParams(params)
    val message = Query(context, entity, projection, entityType, username)
    perRequest(context, queryHandler, message)
  }
}

trait AddRouteHelper extends BaseRoute {

  def postPath(_path: String) =
  post &
  versionedPath(_path) &
  authenticationDirectives.usernameFromRequest()

  def validatePostRoute(entity: AgoraEntity, path: String): Directive0 = {
    validateEntityType(entity.entityType, path) &
    validate(entity.payload.get.nonEmpty, "You must supply a payload.") &
    validate(entity.snapshotId.isEmpty, "You cannot specify a snapshotId. It will be assigned by the system.")
  }

  def completeWithPerRequest(context: RequestContext,
                             entity: AgoraEntity,
                             username: String,
                             path: String,
                             addHandler: Props ) = {
    addUserIfNotInDatabase(username)
    val entityWithType = AgoraEntityType.byPath(path) match {
      case AgoraEntityType.Configuration => entity.addEntityType(Option(AgoraEntityType.Configuration))
      case _ => entity
    }
    perRequest(context, addHandler, Add(context, entityWithType, username))
  }
}

trait RouteUtil extends Directives {

  def validateEntityType(entityType: Option[AgoraEntityType.EntityType], path: String): Directive0 = {
    val possibleTypes = AgoraEntityType.byPath(path)

    if (entityType.isDefined) {
      validate(possibleTypes.contains(entityType.get),
        s"You can't perform operation for entity type $entityType.get at path /$path.")
    }
    else {
      pass
    }
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
