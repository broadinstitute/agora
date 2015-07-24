
package org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.storage.model.{BucketAccessControl, StorageObject, Bucket}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions.{Create, Nothing}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraPermissions, AuthorizationProvider}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsClient._
import spray.http.StatusCodes._
import scala.util.{Success, Failure}
import scala.collection.JavaConverters._

import scala.util.Try

object GcsAuthorizationProvider extends AuthorizationProvider {
  import GcsAuthHelpers._

  override def namespaceAuthorization(entity: AgoraEntity, username: String): AgoraPermissions = {
    val bucket = entityToBucket(entity)
    val isBucketFound = doesBucketExist(bucket)

    if (!isBucketFound) AgoraPermissions(Create)
    else getBucketPermissions(bucket, username)
  }

  override def entityAuthorization(entity: AgoraEntity, username: String): AgoraPermissions = {
      val _object = entityToObject(entity)
      val isObjectFound = doesObjectExist(_object, username)

      if (!isObjectFound) AgoraPermissions(Create)
      else getObjectPermissions(_object, username)
  }

  override def createEntityAuthorizations(entity: AgoraEntity, username: String): Unit = {
    val bucket = entityToBucket(entity)
    val _object = entityToObject(entity)
    val bucketAcl = userToBucketAcl(username, GcsRole.Owner)
    val objectAcl = userToObjectAcl(username, GcsRole.Owner)

    val bucketResponse = getOrCreateBucket(bucket)
    val bucketAcls = bucketResponse.getAcl

    // Convert java list to scala
    val scalaBucketAcls = bucketAcls.asScala.toList
      .map(acl => new BucketAccessControl().setEntity(acl.getEntity).setRole(acl.getRole))

    if (!scalaBucketAcls.contains(bucketAcl)) {
      bucketAcls.add(bucketAcl)
      patchBucketAcls(bucket, bucketAcls).execute()
    }

    val objectResponse = createObject(_object).setProjection("full").execute()
    val objectAcls = objectResponse.getAcl

    objectAcls.add(objectAcl)
    patchObjectAcls(_object, objectAcls).execute()
  }

  def getOrCreateBucket(bucket: Bucket): Bucket = {
    if (doesBucketExist(bucket)) getBucket(bucket).setProjection("full").execute()
    else createBucket(bucket).setProjection("full").execute()
  }
}

object GcsAuthHelpers {

  //namespaceAuthorization helpers
  def doesBucketExist(bucket: Bucket): Boolean = {
    val bucketFound = Try(getBucket(bucket).execute())

    bucketFound match {
      case Success(b: Bucket)  => true
      case Failure(ex: GoogleJsonResponseException) =>
        if (ex.getDetails.getCode == NotFound.intValue) false
        else true
    }
  }

  def getBucketPermissions(bucket: Bucket, username: String): AgoraPermissions = {
    val bucketAcl = Try(getBucketAcl(bucket, username).execute())

    bucketAcl match {
      case Success(acl) =>
        val role = GcsBucketRole(acl.getRole)
        RoleTranslator.gcsBucketToNamespacePermissions(role)

      case Failure(ex) => AgoraPermissions(Nothing)
    }
  }


  //entityAuthorization helpers
  def doesObjectExist(_object: StorageObject, username: String): Boolean = {
    val objectFound = Try(getObject(_object).execute())

    objectFound match {
      case Success(o: StorageObject) => true
      case Failure(ex: GoogleJsonResponseException) =>
        if (ex.getDetails.getCode == NotFound.intValue) false
        else true
    }
  }

  def getObjectPermissions(_object: StorageObject, username: String): AgoraPermissions = {
    val objectAcl = Try(getObjectAcl(_object, username).execute())

    objectAcl match {
      case Success(acl) =>
        val role = GcsObjectRole(acl.getRole)
        RoleTranslator.gcsObjectToEntityPermissions(role)

      case Failure(ex) => AgoraPermissions(Nothing)
    }
  }

}


