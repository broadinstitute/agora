package org.broadinstitute.dsde.agora.server.model

import scalaz._
import scalaz.Scalaz._

/**
 * Included and excluded field projections for an AgoraEntity.
 */
case class AgoraEntityProjection(includedFields: Seq[String], excludedFields: Seq[String]) {
  AgoraEntityProjection.validate(this) match {
    case Success(_) => this
    case Failure(errors) => throw new IllegalArgumentException(s"Projection is not valid: Errors: $errors")
  }

  def totalFields = includedFields.size + excludedFields.size
}

object AgoraEntityProjection {
  val RequiredProjectionFields = Seq[String]("namespace", "name", "snapshotId", "entityType")

  // Scalaz type validation of agora entity fields.
  def validate(projection: AgoraEntityProjection): ValidationNel[String, Boolean] = {

    def validateRequiredFields(projection: AgoraEntityProjection): ValidationNel[String, AgoraEntityProjection] = {
      val overlapFields = projection.excludedFields intersect RequiredProjectionFields

      if (overlapFields.isEmpty) projection.successNel[String]
      else s"$RequiredProjectionFields cannot be in excluded fields.".failureNel[AgoraEntityProjection]
    }

    def validateOnlyIncludedOrExcluded(projection: AgoraEntityProjection): ValidationNel[String, AgoraEntityProjection] = {
      if (projection.includedFields.isEmpty || projection.excludedFields.isEmpty)
        projection.successNel[String]
      else
        "Cannot specify included and excluded fields. Must choose one.".failureNel[AgoraEntityProjection]
    }

    val requiredFields = validateRequiredFields(projection)
    val onlyIncludeOrExclude = validateOnlyIncludedOrExcluded(projection)

    (requiredFields |@| onlyIncludeOrExclude) {(requiredFields, onlyIncludeOrExclude) => true}

  }
}