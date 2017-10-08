package org.broadinstitute.dsde.agora.server.ga4gh

import org.broadinstitute.dsde.agora.server.exceptions.ValidationException
import org.broadinstitute.dsde.agora.server.ga4gh.Models.ToolDescriptorType.DescriptorType
import org.broadinstitute.dsde.agora.server.ga4gh.Models.{ToolDescriptorType, ToolId}

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



}
