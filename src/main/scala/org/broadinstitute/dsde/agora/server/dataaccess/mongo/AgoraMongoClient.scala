
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.{MongoCredential, ServerAddress}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.mongodb.scala._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object AgoraMongoClient {
  private lazy val mongoClient = getMongoClient

  val TasksCollection = "tasks"
  val WorkflowsCollection = "workflows"
  val ConfigurationsCollection = "configurations"
  val CounterCollection = "counters"

  def getCollection(collectionName: String): MongoCollection[Document] = {
    mongoClient.getDatabase(AgoraConfig.mongoDbDatabase).getCollection(collectionName)
  }

  def getCountersCollection: MongoCollection[Document] = {
    mongoClient.getDatabase(AgoraConfig.mongoDbDatabase).getCollection(CounterCollection)
  }

  def getAllCollections: Seq[MongoCollection[Document]] = {
    Seq(getTasksCollection, getWorkflowsCollection, getCountersCollection, getConfigurationsCollection)
  }

  private def getTasksCollection: MongoCollection[Document] = {
    mongoClient.getDatabase(AgoraConfig.mongoDbDatabase).getCollection(TasksCollection)
  }

  private def getWorkflowsCollection: MongoCollection[Document] = {
    mongoClient.getDatabase(AgoraConfig.mongoDbDatabase).getCollection(WorkflowsCollection)
  }

  private def getConfigurationsCollection: MongoCollection[Document] = {
    mongoClient.getDatabase(AgoraConfig.mongoDbDatabase).getCollection(ConfigurationsCollection)
  }

  def getCollectionsByEntityType(entityTypes: Seq[AgoraEntityType.EntityType]): Seq[MongoCollection[Document]] = {
    entityTypes.flatMap {
      entityType => getCollectionsByEntityType(Option(entityType))
    }
  }

  def getCollectionsByEntityType(entityType: Option[AgoraEntityType.EntityType]): Seq[MongoCollection[Document]] = {
    entityType match {
      case Some(AgoraEntityType.Task) =>
        Seq(getTasksCollection)
      case Some(AgoraEntityType.Workflow) =>
        Seq(getWorkflowsCollection)
      case Some(AgoraEntityType.Configuration) =>
        Seq(getConfigurationsCollection)
      case _ =>
        Seq(getConfigurationsCollection, getTasksCollection, getWorkflowsCollection)
    }
  }

  private[mongo] var mongoDbTestConnectionString: Option[ConnectionString] = None

  private def getMongoClient: MongoClient = {
    val mongoClientSettings = MongoClientSettings.builder()

    if (AgoraConfig.mongoDbTestContainerEnabled) {

      mongoDbTestConnectionString match {
        case Some(connectionString) =>
          mongoClientSettings
            .applyToClusterSettings(clusterSettings => clusterSettings.applyConnectionString(connectionString))

        case None =>
          sys.error(
            "The test in this stack trace should be using a `lazy val` instead " +
              "of a `val` to allow the TestContainer MongoDB to start first. " +
              "See `lazy val MethodsDbTest.mongoTestCollection` for an example."
          )
      }

    } else {

      val serverList =
        for {
          (host, port) <- AgoraConfig.mongoDbHosts zip AgoraConfig.mongoDbPorts
        } yield {
          new ServerAddress(host, port)
        }

      val credentialOption =
        for {
          user <- AgoraConfig.mongoDbUser
          password <- AgoraConfig.mongoDbPassword
        } yield
          MongoCredential
            .createScramSha1Credential(
              user,
              AgoraConfig.mongoDbDatabase,
              password.toCharArray
            )

      mongoClientSettings.applyToClusterSettings(clusterSettings => clusterSettings.hosts(serverList.asJava))
      credentialOption.foreach(mongoClientSettings.credential)

    }

    MongoClient(mongoClientSettings.build())
  }

  def getMongoDBStatus(implicit executionContext: ExecutionContext): Future[Unit] = {
    mongoClient.getDatabase(AgoraConfig.mongoDbDatabase).runCommand(Document("dbstats" -> 1)).toFuture().map(_ => ())
  }
}
