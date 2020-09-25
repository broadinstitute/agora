package org.broadinstitute.dsde.agora.server.model

import cats.data.Validated._
import cats.data.ValidatedNel
import cats.syntax.apply._
import cats.syntax.validated._

/**
 * Included and excluded field projections for an AgoraEntity.
 */
case class AgoraEntityProjection(includedFields: Seq[String], excludedFields: Seq[String]) {
  AgoraEntityProjection.validate(this) match {
    case Valid(_) => this
    case Invalid(errors) => throw new IllegalArgumentException(s"Projection is not valid: Errors: $errors")
  }

  def totalFields: Int = includedFields.size + excludedFields.size
}

object AgoraEntityProjection {
  val RequiredProjectionFields: Seq[String] = Seq[String]("namespace", "name", "snapshotId", "entityType")

  // Scalaz type validation of agora entity fields.
  def validate(projection: AgoraEntityProjection): ValidatedNel[String, Boolean] = {

    def validateRequiredFields(projection: AgoraEntityProjection): ValidatedNel[String, AgoraEntityProjection] = {
      val overlapFields = projection.excludedFields intersect RequiredProjectionFields

      if (overlapFields.isEmpty) projection.validNel[String]
      else s"$RequiredProjectionFields cannot be in excluded fields.".invalidNel[AgoraEntityProjection]
    }

    def validateOnlyIncludedOrExcluded(projection: AgoraEntityProjection): ValidatedNel[String, AgoraEntityProjection] = {
      if (projection.includedFields.isEmpty || projection.excludedFields.isEmpty)
        projection.validNel[String]
      else
        "Cannot specify included and excluded fields. Must choose one.".invalidNel[AgoraEntityProjection]
    }

    val requiredFields = validateRequiredFields(projection)
    val onlyIncludeOrExclude = validateOnlyIncludedOrExcluded(projection)

    (requiredFields, onlyIncludeOrExclude) mapN {(_, _) => true}

  }
}
