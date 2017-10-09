package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.ga4gh.Models.ToolDescriptorType.DescriptorType
import org.broadinstitute.dsde.agora.server.ga4gh.Models.{ToolDescriptorType, ToolId}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}

import scala.util.{Failure, Success, Try}

trait Ga4ghServiceSupport {

  def parseId(id:String): ToolId = {
    val ids = id.split(Models.ID_DELIMITER)
    if (ids.length != 2)
      throw ValidationException(s"This service requires the id to be a colon-delimited namespace and name. Found: $id")

    ToolId(ids(0), ids(1))
  }

  def parseVersionId(versionId:String): Int = {
    Try(versionId.toInt) match {
      case Failure(_) => throw ValidationException(s"This service only supports integer versionId values. Found: $versionId")
      case Success(snapshotId) => snapshotId
    }
  }

  def parseDescriptorType(descriptorType:String): DescriptorType = {
    if (descriptorType != ToolDescriptorType.WDL.toString && descriptorType != ToolDescriptorType.PLAIN_WDL.toString)
      throw ValidationException(s"This service only supports WDL and plain-WDL. Found: $descriptorType")
    ToolDescriptorType.withName(descriptorType)
  }

  // validate url arguments supplied by the user and return an AgoraEntity that can be used as criteria
  // for querying the db.
  def entityFromArguments(id: String, versionId: String): AgoraEntity = {
    argsToEntity(id, Some(versionId))
  }

  def entityFromArguments(id: String): AgoraEntity = {
    argsToEntity(id, None)
  }

  private def argsToEntity(id: String, versionId: Option[String]): AgoraEntity = {
    val snapshotOption:Option[Int] = versionId map parseVersionId
    val toolId = parseId(id)

    AgoraEntity(Some(toolId.namespace), Some(toolId.name), snapshotOption, entityType = Some(AgoraEntityType.Workflow))
  }


}
