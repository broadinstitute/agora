package org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs

import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsRole._

object RoleTranslator {

  def gcsBucketToNamespacePermissions(bucketRole: GcsBucketRole): AgoraPermissions = {
    bucketRole match {
      case GcsBucketRole(Owner) => new AgoraPermissions(Read, Write, Create, Redact, Manage)
      case GcsBucketRole(Writer) => new AgoraPermissions(Read, Write, Create, Redact)
      case GcsBucketRole(Reader) => AgoraPermissions(Read)
      case _ => AgoraPermissions(AgoraPermissions.Nothing) 
    }
  }

  def gcsObjectToEntityPermissions(objectRole: GcsObjectRole): AgoraPermissions = {
    objectRole match {
      case GcsObjectRole(Owner) => new AgoraPermissions(Read, Manage)
      case GcsObjectRole(Reader) => AgoraPermissions(Read)
      case _ => AgoraPermissions(AgoraPermissions.Nothing)
    }
  }

  def namespaceToBucketRole(namespacePermissions: AgoraPermissions): GcsBucketRole = {
    namespacePermissions match {
      case AgoraPermissions(Manage) => GcsBucketRole(Owner)
      case AgoraPermissions(Write) => GcsBucketRole(Writer)
      case AgoraPermissions(Read) => GcsBucketRole(Reader)
      case _ => GcsBucketRole(GcsRole.Nothing)
    }
  }

  def methodToObjectRole(methodPermissions: AgoraPermissions): GcsObjectRole = {
    methodPermissions match {
      case AgoraPermissions(Manage) => GcsObjectRole(Owner)
      case AgoraPermissions(Read) => GcsObjectRole(Reader)
      case _ => GcsObjectRole(GcsRole.Nothing)
    }
  }
}
