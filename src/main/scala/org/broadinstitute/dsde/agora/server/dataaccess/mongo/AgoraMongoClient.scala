
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.{MongoClient, MongoCollection}
import com.mongodb.{MongoCredential, ServerAddress}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object AgoraMongoClient {
  private val mongoClient = getMongoClient

  val TasksCollection = "tasks"
  val WorkflowsCollection = "workflows"
  val ConfigurationsCollection = "configurations"
  val CounterCollection = "counters"

  def getCollection(collectionName: String): MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(collectionName)
  }

  def getCountersCollection: MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(CounterCollection)
  }

  def getAllCollections: Seq[MongoCollection] = {
    Seq(getTasksCollection, getWorkflowsCollection, getCountersCollection, getConfigurationsCollection)
  }

  private def getTasksCollection: MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(TasksCollection)
  }

  private def getWorkflowsCollection: MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(WorkflowsCollection)
  }

  private def getConfigurationsCollection: MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(ConfigurationsCollection)
  }

  def getCollectionsByEntityType(entityTypes: Seq[AgoraEntityType.EntityType]): Seq[MongoCollection] = {
    entityTypes.flatMap {
      entityType => getCollectionsByEntityType(Option(entityType))
    }
  }

  def getCollectionsByEntityType(entityType: Option[AgoraEntityType.EntityType]): Seq[MongoCollection] = {
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

  private def getMongoClient: MongoClient = {
    val serverList = for{ (host, port) <- AgoraConfig.mongoDbHosts zip AgoraConfig.mongoDbPorts } yield {
      new ServerAddress(host, port)
    }

    val user: Option[String] = AgoraConfig.mongoDbUser
    val password: Option[String] = AgoraConfig.mongoDbPassword

    (user, password) match {
      case (Some(userName), Some(userPassword)) =>
        val credentials = MongoCredential.createScramSha1Credential(
          userName,
          AgoraConfig.mongoDbDatabase,
          userPassword.toCharArray
        )
        MongoClient(serverList, List(credentials))
      case _ =>
        MongoClient()
    }

  }

  def getMongoDBStatus: Unit = {
    mongoClient.getDB(AgoraConfig.mongoDbDatabase).getStats()
  }
}
