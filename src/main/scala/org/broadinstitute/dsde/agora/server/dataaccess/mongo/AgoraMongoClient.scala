
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.{MongoClient, MongoCollection, MongoDB}

object AgoraMongoClient {

  val MethodsCollection = "methods"
  val AgoraDatabase = "agora"

  def getCollection(mongoDb: MongoDB, collectionName: String): MongoCollection = {
    mongoDb(collectionName)
  }

  def getMethodsCollection(client: MongoClient): MongoCollection = {
    client(AgoraDatabase)(MethodsCollection)
  }
}