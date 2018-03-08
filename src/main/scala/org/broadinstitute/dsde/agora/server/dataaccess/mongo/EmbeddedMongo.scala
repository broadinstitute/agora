
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import _root_.de.flapdoodle.embed.mongo.distribution.Version
import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import org.broadinstitute.dsde.agora.server.AgoraConfig

object EmbeddedMongo extends MongoEmbedDatabase {
  var mongodProps: MongodProps = null

  def startMongo() = {
    println(s"Starting embedded mongo db instance.")
    if (mongodProps == null || !mongodProps.mongodProcess.isProcessRunning) {
      mongodProps = mongoStart(port = 27017, version = Version.V3_0_6)
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
