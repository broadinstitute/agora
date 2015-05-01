
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.{MongoClient, MongoCollection, MongoDB}
import com.mongodb.{MongoCredential, ServerAddress}
import org.broadinstitute.dsde.agora.server.Agora

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
    val server = new ServerAddress(Agora.server.mongoDbHost, Agora.server.mongoDbPort)

    val user: Option[String] = Agora.server.mongoDbUser
    val password: Option[String] = Agora.server.mongoDbPassword

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