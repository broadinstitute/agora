package org.broadinstitute.dsde.agora.server.ga4gh

import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}

import scala.concurrent.{ExecutionContext, Future}

trait Ga4ghQueryHandler extends LazyLogging {

  implicit val dataSource: PermissionsDataSource
  implicit val ec: ExecutionContext
  def agoraBusiness = new AgoraBusiness(dataSource)(ec)

  // Include entity payloads in returned results
  val DefaultProjection = Some(AgoraEntityProjection(Seq[String]("payload", "synopsis"), Seq.empty[String]))

  def queryPublicSingle(entity: AgoraEntity): Future[ToolVersion] =
    queryPublicSingleEntity(entity) map (ToolVersion(_))

  def queryPublicSingleEntity(entity: AgoraEntity): Future[AgoraEntity] = {
    val entityTypes = Seq(entity.entityType.getOrElse(throw ValidationException("need an entity type")))
    agoraBusiness.findSingle(entity, entityTypes, AccessControl.publicUser)
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
