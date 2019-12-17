package org.broadinstitute.dsde.agora.server.ga4gh

import akka.actor.typed.ActorRef
import com.typesafe.scalalogging.LazyLogging
import org.broadinstitute.dsde.agora.server.actor.AgoraGuardianActor
import org.broadinstitute.dsde.agora.server.business.{AgoraBusiness, AgoraBusinessExecutionContext}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}

import scala.concurrent.{ExecutionContext, Future}

trait Ga4ghQueryHandler extends LazyLogging {

  def dataSource: PermissionsDataSource
  def agoraGuardian: ActorRef[AgoraGuardianActor.Command]
  def agoraBusiness = new AgoraBusiness(dataSource, agoraGuardian)

  // Include entity payloads in returned results
  val DefaultProjection: Option[AgoraEntityProjection] =
    Option(AgoraEntityProjection(Seq[String]("payload", "synopsis"), Seq.empty[String]))

  def queryPublicSingle(entity: AgoraEntity)
                       (implicit
                        executionContext: ExecutionContext,
                        agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Future[ToolVersion] =
    queryPublicSingleEntity(entity) map (ToolVersion(_))

  def queryPublicSingleEntity(entity: AgoraEntity)
                             (implicit
                              agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Future[AgoraEntity] = {
    val entityTypes = Seq(entity.entityType.getOrElse(throw ValidationException("need an entity type")))
    agoraBusiness.findSingle(entity, entityTypes, AccessControl.publicUser)(
      agoraBusinessExecutionContext.executionContext
    )
  }

  def queryPublic(agoraSearch: AgoraEntity)
                 (implicit
                  executionContext: ExecutionContext,
                  agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Future[Seq[ToolVersion]] = {
    agoraBusiness.find(agoraSearch, DefaultProjection, Seq(AgoraEntityType.Workflow), AccessControl.publicUser)(
      agoraBusinessExecutionContext.executionContext
    ) map { entities =>
      entities map ToolVersion.apply
    }
  }

  def queryPublicTool(agoraSearch: AgoraEntity)
                     (implicit
                      executionContext: ExecutionContext,
                      agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Future[Tool] = {
    agoraBusiness.find(agoraSearch, DefaultProjection, Seq(AgoraEntityType.Workflow), AccessControl.publicUser)(
      agoraBusinessExecutionContext.executionContext
    ) map { entities =>
      Tool(entities)
    }
  }

  def queryPublicTools()
                      (implicit
                       executionContext: ExecutionContext,
                       agoraBusinessExecutionContext: AgoraBusinessExecutionContext): Future[Seq[Tool]] = {
    agoraBusiness.find(AgoraEntity(), DefaultProjection, Seq(AgoraEntityType.Workflow), AccessControl.publicUser)(
      agoraBusinessExecutionContext.executionContext
    ) map { allentities =>
      val groupedSnapshots = allentities.groupBy( ae => (ae.namespace,ae.name))
      val tools:Seq[Tool] = (groupedSnapshots.values map { entities => Tool(entities )}).toSeq
      tools.sortBy(_.id)
    }
  }
}
