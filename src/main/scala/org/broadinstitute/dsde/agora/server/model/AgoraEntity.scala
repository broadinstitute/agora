
package org.broadinstitute.dsde.agora.server.model

import java.util.Date

case class AgoraEntity(var id: Option[Int] = None,
                       namespace: Option[String] = None,
                       name: Option[String] = None,
                       synopsis: Option[String] = None,
                       documentation: Option[String] = None,
                       owner: Option[String] = None,
                       createDate: Option[Date] = None,
                       payload: Option[String] = None
                        )
