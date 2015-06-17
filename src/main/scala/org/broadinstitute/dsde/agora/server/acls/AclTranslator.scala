package org.broadinstitute.dsde.agora.server.acls

import org.broadinstitute.dsde.agora.server.business.AgoraAcl
import org.broadinstitute.dsde.agora.server.business.AgoraPermissions._

/**
 * Created by dshiga on 6/16/15.
 */
object AclTranslator {

  def gcsRolesToAgoraAcl(bucketRole: GcsRole, objectRole: Option[GcsRole]): AgoraAcl = {
    val canRead = bucketRole.isReader || objectRole.nonEmpty && objectRole.get.isReader
    val canWrite = bucketRole.isWriter
    val canRedact = bucketRole.isWriter
    val canCreate = bucketRole.isWriter
    val canManage = bucketRole.isOwner

    var agoraAcl = new AgoraAcl()
    if (canRead) {
      agoraAcl = agoraAcl.addPermissions(Read)
    }
    if (canWrite) {
      agoraAcl = agoraAcl.addPermissions(Write)
    }
    if (canRedact) {
      agoraAcl = agoraAcl.addPermissions(Redact)
    }
    if (canCreate) {
      agoraAcl = agoraAcl.addPermissions(Create)
    }
    if (canManage) {
      agoraAcl = agoraAcl.addPermissions(Manage)
    }
    agoraAcl
  }
}
