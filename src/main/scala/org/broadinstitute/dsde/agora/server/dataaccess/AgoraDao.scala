
package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AgoraDao {
  def insert(entity: AgoraEntity): AgoraEntity
  def findByName(name: String): Seq[AgoraEntity]
  def findPayloadByRegex(regex: String): Seq[AgoraEntity]

  def find(namespace: String, name: String, id: Int): AgoraEntity
}
