
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
    //for some unknown reason the mongo db needs to wait a bit before it can be started back up
    //this is likely a java driver issue and should be revisited when the driver is updated.
    Thread.sleep(5000)
  }
}
