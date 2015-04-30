
package org.broadinstitute.dsde.agora.server.model

import java.util.Date

case class AgoraMetadata(id: Option[Integer] = None, namespace: String, name: String, synopsis: String,
                         documentation: String, owner: String, createDate: Date)

case class AgoraEntity(metadata: AgoraMetadata, payload: String)
