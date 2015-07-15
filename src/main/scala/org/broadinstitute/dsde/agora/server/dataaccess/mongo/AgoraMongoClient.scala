
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.{MongoClient, MongoCollection}
import com.mongodb.{MongoCredential, ServerAddress}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType

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

  private def getTasksCollection: MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(TasksCollection)
  }

  private def getWorkflowsCollection: MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(WorkflowsCollection)
  }

  private def getConfigurationsCollection: MongoCollection = {
    mongoClient(AgoraConfig.mongoDbDatabase)(ConfigurationsCollection)
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
    val server = new ServerAddress(AgoraConfig.mongoDbHost, AgoraConfig.mongoDbPort)

    val user: Option[String] = AgoraConfig.mongoDbUser
    val password: Option[String] = AgoraConfig.mongoDbPassword


    (user, password) match {
      case (Some(userName), Some(userPassword)) =>
        val credentials = MongoCredential.createMongoCRCredential(
          userName,
          AgoraConfig.mongoDbDatabase,
          userPassword.toCharArray
        )
        MongoClient(server, List(credentials))
      case _ =>
        MongoClient()
    }

  }
}
