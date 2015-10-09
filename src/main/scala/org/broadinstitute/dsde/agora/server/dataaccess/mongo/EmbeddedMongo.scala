
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import _root_.de.flapdoodle.embed.mongo.distribution.Version
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import org.broadinstitute.dsde.agora.server.AgoraConfig

object EmbeddedMongo extends MongoEmbedDatabase {
  var mongodProps: MongodProps = null

  def startMongo() = {
    println(s"Starting embedded mongo db instance.")
    if (mongodProps == null || !mongodProps.mongodProcess.isProcessRunning) {
      mongodProps = mongoStart(port = AgoraConfig.mongoDbPort, version = Version.V2_7_1)
    }
  }

  def stopMongo() = {
    println(s"Stopping embedded mongo db instance.")
    mongoStop(mongodProps)
  }

  def isRunning: Boolean = {
    mongodProps != null && mongodProps.mongodProcess.isProcessRunning
  }
}
