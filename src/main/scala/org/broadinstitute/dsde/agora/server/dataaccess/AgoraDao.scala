
package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AgoraDao {
  def insert(entity: AgoraEntity): AgoraEntity
  def find(entity: AgoraEntity): Seq[AgoraEntity]
  def findSingle(entity: AgoraEntity): AgoraEntity
}
