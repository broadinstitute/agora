package org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs

import org.broadinstitute.dsde.agora.server.model._
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol

object GcsApiJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val ProjectTeamFormat = jsonFormat2(ProjectTeam)

  implicit val GoogleAccessControlsFormat = jsonFormat13(GoogleAccessControls)

  implicit val OwnerFormat = jsonFormat2(Owner)

  implicit val WebsiteFormat = jsonFormat2(Website)

  implicit val LoggingFormat = jsonFormat2(Logging)

  implicit val VersioningFormat = jsonFormat1(Versioning)

  implicit val CorsFormat = jsonFormat4(Cors)

  implicit val ActionFormat = jsonFormat1(Action)

  implicit val ConditionFormat = jsonFormat4(Condition)

  implicit val RuleFormat = jsonFormat2(Rule)

  implicit val LifecycleFormat = jsonFormat1(Lifecycle)

  implicit val BucketResourceFormat = jsonFormat14(BucketResource)
}
