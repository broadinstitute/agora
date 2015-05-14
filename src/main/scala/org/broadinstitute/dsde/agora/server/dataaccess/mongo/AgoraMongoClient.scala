
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.{MongoClient, MongoCollection, MongoDB}
import com.mongodb.{MongoCredential, ServerAddress}
import org.broadinstitute.dsde.agora.server.{AgoraConfig, Agora}

object AgoraMongoClient {

  val MethodsCollection = "methods"
  val AgoraDatabase = "agora"

  def getCollection(mongoDb: MongoDB, collectionName: String): MongoCollection = {
    mongoDb(collectionName)
  }

  def getMethodsCollection(client: MongoClient): MongoCollection = {
    client(AgoraDatabase)(MethodsCollection)
  }

  def getMongoClient: MongoClient = {
    val server = new ServerAddress(AgoraConfig.mongoDbHost, AgoraConfig.mongoDbPort)

    val user: Option[String] = AgoraConfig.mongoDbUser
    val password: Option[String] = AgoraConfig.mongoDbPassword

    (user, password) match {
      case (Some(userName), Some(userPassword)) =>
        val credentials = MongoCredential.createMongoCRCredential(
          userName,
          AgoraDatabase,
          userPassword.toCharArray
        )
        MongoClient(server, List(credentials))
      case _ =>
        MongoClient()
    }

  }
}
