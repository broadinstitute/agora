package org.broadinstitute.dsde.agora.server.webservice.routes

import akka.http.scaladsl.server._
import org.broadinstitute.dsde.agora.server.AgoraConfig.{authenticationDirectives, version}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}

import scala.concurrent.ExecutionContext
import scala.util.Try

trait RouteHelpers extends AddRouteHelper with QueryRouteHelper

trait BaseRoute extends RouteUtil  {
  def getUserFromParams(params: Map[String, String]): String = {
    val user = params.get("user")
    if (user.isDefined)
      user.get
    else
      throw new IllegalArgumentException("Missing params: user and/or role.")
  }

  def versionedPath[L](pm: PathMatcher[L]): Directive[L] = path("api" / version / pm)
}

trait QueryRouteHelper extends BaseRoute {
  def matchQuerySingleRoute(_path: String)(implicit ec: ExecutionContext) =
    versionedPath(_path / Segment / Segment / IntNumber) & authenticationDirectives.usernameFromRequest(())
  
  def matchQueryRoute(_path: String)(implicit ec: ExecutionContext) =
    get & versionedPath(_path) & authenticationDirectives.usernameFromRequest(())

  def entityFromParams(params: Map[String, List[String]]): AgoraEntity = AgoraEntity(
    namespace       = params.getOrElse("namespace", Nil).headOption,
    name            = params.getOrElse("name", Nil).headOption,
    snapshotId      = params.getOrElse("snapshotId", Nil).headOption.toIntOption,
    snapshotComment = params.getOrElse("snapshotComment", Nil).headOption,
    synopsis        = params.getOrElse("synopsis", Nil).headOption,
    documentation   = params.getOrElse("documentation", Nil).headOption,
    owner           = params.getOrElse("owner", Nil).headOption,
    payload         = params.getOrElse("payload", Nil).headOption,
    url             = params.getOrElse("url", Nil).headOption,
    entityType      = params.getOrElse("entityType", Nil).headOption.toAgoraEntityOption
  )

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
}

trait AddRouteHelper extends BaseRoute {
  def postPath(_path: String)(implicit ec: ExecutionContext) = {
    post & versionedPath(_path) & authenticationDirectives.usernameFromRequest(())
  }

  def validatePostRoute(entity: AgoraEntity, path: String): Directive0 = {
    validateEntityType(entity.entityType, path) &
    validate(entity.payload.nonEmpty && entity.payload.get.trim.nonEmpty, "You must supply a payload.") &
    validate(entity.snapshotId.isEmpty, "You cannot specify a snapshotId. It will be assigned by the system.") &
    validate(entity.synopsis.isEmpty || entity.synopsis.get.length <= 80, "Synopsis must be less than 80 chars" ) &
    validate(entity.namespace.nonEmpty && entity.namespace.get.trim.nonEmpty, "Namespace cannot be empty") &
    validate(entity.name.nonEmpty && entity.name.get.trim.nonEmpty, "Name cannot be empty") &
    validate(entity.documentation.isEmpty || entity.documentation.get.getBytes.size <= 10000, "Documentation must be less than 10kb")
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
