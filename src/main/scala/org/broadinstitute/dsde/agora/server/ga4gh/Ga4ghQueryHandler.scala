package org.broadinstitute.dsde.agora.server.ga4gh

import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, PermissionBusiness}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

trait Ga4ghQueryHandler extends LazyLogging {

  implicit val dataSource: PermissionsDataSource
  implicit val ec: ExecutionContext
  val permissionBusiness = new PermissionBusiness(dataSource)(ec)
  val agoraBusiness = new AgoraBusiness(dataSource)(ec)

  // Include entity payloads in returned results
  val DefaultProjection = Some(AgoraEntityProjection(Seq[String]("payload", "synopsis"), Seq.empty[String]))

  def queryPublicSingle(entity: AgoraEntity): Future[ToolVersion] = {
    val entityTypes = Seq(entity.entityType.getOrElse(throw ValidationException("need an entity type")))
    agoraBusiness.findSingle(entity, entityTypes, AccessControl.publicUser) map { foundEntity =>
      ToolVersion(foundEntity)
    }
  }

  def queryPublicSinglePayload(entity: AgoraEntity, descriptorType: ToolDescriptorType.DescriptorType): Future[JsValue] = {
    val entityTypes = Seq(entity.entityType.getOrElse(throw ValidationException("need an entity type")))
    agoraBusiness.findSingle(entity, entityTypes, AccessControl.publicUser) map { foundEntity =>
      descriptorType match {
        case ToolDescriptorType.WDL =>
          // the url we return here is known to be incorrect in FireCloud (GAWB-1741).
          // we return it anyway because it still provides some information, even if it
          // requires manual user intervention to work.
          ToolDescriptor(foundEntity).toJson
        case ToolDescriptorType.PLAIN_WDL =>
          JsString(foundEntity.payload.getOrElse(""))
      }
    }
  }

  def queryPublic(agoraSearch: AgoraEntity): Future[Seq[ToolVersion]] = {
    agoraBusiness.find(agoraSearch, DefaultProjection, Seq(AgoraEntityType.Workflow), AccessControl.publicUser) map { entities =>
      entities map ToolVersion.apply
    }
  }

  def queryPublicTool(agoraSearch: AgoraEntity): Future[Tool] = {
    agoraBusiness.find(agoraSearch, DefaultProjection, Seq(AgoraEntityType.Workflow), AccessControl.publicUser) map { entities =>
      Tool(entities)
    }
  }

  def queryPublicTools(): Future[Seq[Tool]] = {
    agoraBusiness.find(AgoraEntity(), DefaultProjection, Seq(AgoraEntityType.Workflow), AccessControl.publicUser) map { allentities =>
      val groupedSnapshots = allentities.groupBy( ae => (ae.namespace,ae.name))
      val tools:Seq[Tool] = (groupedSnapshots.values map { entities => Tool(entities )}).toSeq
      tools.sortBy(_.id)
    }
  }
}
