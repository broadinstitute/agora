package org.broadinstitute.dsde.agora.server.ga4gh

import akka.pattern.pipe
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghServiceMessages._
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.{PerRequestMessage, RequestComplete}
import org.broadinstitute.dsde.agora.server.webservice.handlers.QueryHandler
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.routing.RequestContext

import scala.concurrent.{ExecutionContext, Future}

class Ga4ghQueryHandler(dataSource: PermissionsDataSource, override implicit val ec: ExecutionContext)
  extends QueryHandler(dataSource, ec) {

  override def receive: akka.actor.Actor.Receive = {
    case QueryPublicSingle(requestContext: RequestContext, entity: AgoraEntity) =>
      queryPublicSingle(requestContext, entity) pipeTo context.parent

    case QueryPublicSinglePayload(requestContext: RequestContext, entity: AgoraEntity, descriptorType: ToolDescriptorType.DescriptorType) =>
      queryPublicSinglePayload(requestContext, entity, descriptorType) pipeTo context.parent

    case QueryPublic(requestContext: RequestContext, agoraSearch: AgoraEntity) =>
      queryPublic(requestContext, agoraSearch) pipeTo context.parent

    case QueryPublicTool(requestContext: RequestContext, agoraSearch: AgoraEntity) =>
      queryPublicTool(requestContext, agoraSearch) pipeTo context.parent

    case QueryPublicTools(requestContext: RequestContext) =>
      queryPublicTools(requestContext) pipeTo context.parent


  }

  def queryPublicSingle(requestContext: RequestContext, entity: AgoraEntity): Future[PerRequestMessage] = {
    val entityTypes = Seq(entity.entityType.getOrElse(throw ValidationException("need an entity type")))
    agoraBusiness.findSingle(entity, entityTypes, AccessControl.publicUser) map { foundEntity =>
      RequestComplete(ToolVersion(foundEntity))
    }
  }

  def queryPublicSinglePayload(requestContext: RequestContext, entity: AgoraEntity, descriptorType: ToolDescriptorType.DescriptorType): Future[PerRequestMessage] = {
    val entityTypes = Seq(entity.entityType.getOrElse(throw ValidationException("need an entity type")))
    agoraBusiness.findSingle(entity, entityTypes, AccessControl.publicUser) map { foundEntity =>
      descriptorType match {
        case ToolDescriptorType.WDL =>
          // the url we return here is known to be incorrect in FireCloud (GAWB-1741).
          // we return it anyway because it still provides some information, even if it
          // requires manual user intervention to work.
          val result = ToolDescriptor(foundEntity)
          RequestComplete(result)
        case ToolDescriptorType.PLAIN_WDL =>
          RequestComplete(foundEntity.payload.getOrElse(""))
      }
    }
  }

  def queryPublic(requestContext: RequestContext,
                  agoraSearch: AgoraEntity): Future[PerRequestMessage] = {
    agoraBusiness.find(agoraSearch, None, Seq(AgoraEntityType.Workflow), AccessControl.publicUser) map { entities =>
      val toolVersions = entities map ToolVersion.apply
      RequestComplete(toolVersions)
    }
  }

  def queryPublicTool(requestContext: RequestContext,
                      agoraSearch: AgoraEntity): Future[PerRequestMessage] = {
    agoraBusiness.find(agoraSearch, None, Seq(AgoraEntityType.Workflow), AccessControl.publicUser) map { entities =>
      RequestComplete(Tool(entities))
    }
  }

  def queryPublicTools(requestContext: RequestContext): Future[PerRequestMessage] = {
    agoraBusiness.find(AgoraEntity(), None, Seq(AgoraEntityType.Workflow), AccessControl.publicUser) map { allentities =>
      val groupedSnapshots = allentities.groupBy( ae => (ae.namespace,ae.name))
      val tools:Seq[Tool] = (groupedSnapshots.values map { entities => Tool(entities )}).toSeq
      RequestComplete(tools.sortBy(_.id))
    }
  }
}
