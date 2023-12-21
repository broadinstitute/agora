package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.dimafeng.testcontainers.MongoDBContainer
import com.mongodb.ConnectionString
import com.typesafe.scalalogging.StrictLogging
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.mongodb.scala.{Document, MongoCollection}
import org.testcontainers.containers.wait.strategy.Wait

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object TestAgoraMongoClient extends StrictLogging {
  import scala.concurrent.ExecutionContext.Implicits.global

  // Start up in the object static initializer so that we can set AgoraMongoClient.testConnectionString
  {
    if (AgoraConfig.mongoDbTestContainerEnabled) {
      val imageName = "mongo:7.0.4"
      val mongoDBContainer = MongoDBContainer(imageName)

      // We may remove this when this is released:
      //   https://github.com/testcontainers/testcontainers-java/pull/3083
      mongoDBContainer.container.waitingFor(
        Wait.forLogMessage("(?i).*waiting for connections.*", 1)
      )

      logger.info(s"Pulling '$imageName' and starting a MongoDB TestContainer...")
      mongoDBContainer.start()

      val url = mongoDBContainer.replicaSetUrl
      logger.info(s"Started MongoDB instance on '$url'.")
      AgoraMongoClient.mongoDbTestConnectionString = Option(new ConnectionString(url))
    }
  }

  def ensureRunning(): Unit = {
    // Initialization of the TestContainer is done in the object static initializer above
  }

  def clean(collections: Seq[MongoCollection[Document]]): Unit = {
    val deletes = collections.map(_.deleteMany(Document.empty).toFuture())
    Await.result(Future.sequence(deletes), 1.minute)
  }
}
