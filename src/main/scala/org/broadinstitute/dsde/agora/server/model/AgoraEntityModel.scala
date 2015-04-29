
package org.broadinstitute.dsde.agora.server.model

import java.util.Date

case class AgoraMetadata(namespace: String, name: String, synopsis: String,
                         documentation: String, owner: String, createDate: Date, var id: Option[Int] = None)

case class AgoraEntity(metadata: AgoraMetadata, payload: String)
