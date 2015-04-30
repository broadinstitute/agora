
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.{MongoClient, MongoCollection, MongoDB}

object AgoraMongoClient {

  def getDatabase(client: MongoClient, databaseName: String): MongoDB = {
    client(databaseName)
  }

  def getCollection(mongoDB: MongoDB, collectionName: String): MongoCollection = {
    mongoDB(collectionName)
  }
}