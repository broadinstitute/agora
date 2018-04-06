package org.broadinstitute.dsde.agora.server.ga4gh

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.PermissionsDataSource
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import spray.json._

import scala.util.Failure
import scala.util.Success
import scala.concurrent.{ExecutionContextExecutor, Future}

class Ga4ghService(permissionsDataSource: PermissionsDataSource) extends Ga4ghQueryHandler with Ga4ghServiceSupport with SprayJsonSupport with DefaultJsonProtocol with LazyLogging {

  // dataSource and ec Required for Ga4ghQueryHandler
  implicit val dataSource: PermissionsDataSource = permissionsDataSource
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  def routes: Route =
    pathPrefix("ga4gh" / "v1") {
      get {
        path("metadata") {
          complete(StatusCodes.OK, Metadata())
        } ~
        path("tool-classes") {
          val toolClassesResponse:Seq[ToolClass] = Seq(ToolClass.apply(Some(AgoraEntityType.Workflow)))
          complete(toolClassesResponse)
        } ~
        path("tools") {
          // TODO: query params and response headers
          complete(queryPublicTools())
        } ~
        path("tools" / Segment) { id =>
          complete(queryPublicTool(entityFromArguments(id)))
        } ~
        path("tools" / Segment / "versions") { id =>
          complete(queryPublic(entityFromArguments(id)))
        } ~
        path("tools" / Segment / "versions" / Segment) { (id, versionId) =>
          complete(queryPublicSingle(entityFromArguments(id, versionId)))
        } ~
        path("tools" / Segment / "versions" / Segment / "dockerfile") { (id, versionId) =>
          complete(StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor") { (id, versionId, descriptorType) =>
          val agoraEntity: Future[AgoraEntity] = queryPublicSingleEntity(entityFromArguments(id, versionId))
          val descriptor = parseDescriptorType(descriptorType)
          onComplete(agoraEntity) {
            case Success(ae) =>
              descriptor match {
                case ToolDescriptorType.WDL =>
                  // the url we return here is known to be incorrect in FireCloud (GAWB-1741).
                  // we return it anyway because it still provides some information, even if it
                  // requires manual user intervention to work.
                  complete(ToolDescriptor(ae))
                case ToolDescriptorType.PLAIN_WDL =>
                  val payload: String = ae.payload.getOrElse("")
                  complete(payload)
              }
            case Failure(ex) => failWith(ex)
          }
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "descriptor" / Segment) { (id, versionId, descriptorType, relativePath) =>
          complete(StatusCodes.NotImplemented)
        } ~
        path("tools" / Segment / "versions" / Segment / Segment / "tests") { (id, versionId, descriptorType) =>
          complete(StatusCodes.NotImplemented)
        }
      }
    }
}
