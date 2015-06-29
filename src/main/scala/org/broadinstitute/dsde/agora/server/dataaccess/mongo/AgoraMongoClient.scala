
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.{MongoClient, MongoCollection, MongoDB}
import com.mongodb.{MongoCredential, ServerAddress}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType

object AgoraMongoClient {

  val TasksCollection = "tasks"
  val WorkflowsCollection = "workflows"
  val ConfigurationsCollection = "configurations"

      
  def getCollection(mongoDb: MongoDB, collectionName: String): MongoCollection = {
    mongoDb(collectionName)
  }

  def getTasksCollection(client: MongoClient): MongoCollection = {
    client(AgoraConfig.mongoDbDatabase)(TasksCollection)
  }

  def getWorkflowsCollection(client: MongoClient): MongoCollection = {
    client(AgoraConfig.mongoDbDatabase)(WorkflowsCollection)
  }

  def getConfigurationsCollection(client: MongoClient): MongoCollection = {
    client(AgoraConfig.mongoDbDatabase)(ConfigurationsCollection)
  }


  def getCollectionsByEntityType(entityType: Option[AgoraEntityType.EntityType]): Seq[MongoCollection] = {
    entityType match {
      case Some(AgoraEntityType.Task) =>
        Seq(getTasksCollection(getMongoClient))
      case Some(AgoraEntityType.Workflow) =>
        Seq(getWorkflowsCollection(getMongoClient))
      case Some(AgoraEntityType.Configuration) =>
        Seq(getConfigurationsCollection(getMongoClient))
      case _ =>
        Seq(getConfigurationsCollection(getMongoClient), getTasksCollection(getMongoClient), getWorkflowsCollection(getMongoClient))
    }
  }

  def getMongoClient: MongoClient = {
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
