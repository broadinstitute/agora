
package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import com.mongodb.casbah.Imports._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.query.Imports
import com.novus.salat._
import com.novus.salat.global._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.model.AgoraEntity

class AgoraMongoDao(collection: MongoCollection) extends AgoraDao {

  def findPayloadByRegex(regex: String): Seq[AgoraEntity] = {
    find("payload" $regex regex)
  }

  def findByName(name: String): Seq[AgoraEntity] = {
    find("metadata.name" $eq name)
  }

  def insert(entity: AgoraEntity): AgoraEntity = {
    val entityDbObject = grater[AgoraEntity].asDBObject(entity)
    collection.insert(entityDbObject)
    find(entityDbObject).head
  }

  def find(query: Imports.DBObject) = {
    (for (entity <- collection.find(query)) yield grater[AgoraEntity].asObject(entity)).toList
  }
}
