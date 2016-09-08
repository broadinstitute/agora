package org.broadinstitute.dsde.agora.server.dataaccess

import org.broadinstitute.dsde.agora.server.dataaccess.mongo.AgoraMongoClient._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient._

import scala.concurrent.Await
/**
  * Created by skwalker on 9/6/16.
  */
class AgoraStatus {
  def status() = {
    getMongoDBStatus
    Await.result(sqlDBStatus, timeout)
  }
}
