package org.broadinstitute.dsde.agora.server.ga4gh

import akka.pattern.pipe

import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, PermissionsDataSource}
import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.webservice.PerRequest.{PerRequestMessage, RequestComplete}
import org.broadinstitute.dsde.agora.server.webservice.handlers.QueryHandler
import org.broadinstitute.dsde.agora.server.webservice.util.ServiceMessages.{QueryPublic, QueryPublicSingle, QueryPublicSinglePayload}
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
          val result = ToolDescriptor(foundEntity.url.getOrElse(""),
            foundEntity.payload.getOrElse(""),
            ToolDescriptorType.WDL)
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

}
