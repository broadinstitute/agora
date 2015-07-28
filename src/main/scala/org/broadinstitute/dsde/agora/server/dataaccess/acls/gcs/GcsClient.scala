package org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.storage.Storage
import com.google.api.services.storage.model.{BucketAccessControl, ObjectAccessControl, StorageObject, Bucket}
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.GoogleCredentialHandler

object GcsClient {
  import GcsClientHelpers._

  val JSON_FACTORY = JacksonFactory.getDefaultInstance()

  val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
  val credentials = GoogleCredentialHandler.credential

  val client = new Storage.Builder(httpTransport, JSON_FACTORY, credentials)
    .setApplicationName("agora").build()

  // Params
  def objectAclFromParams(params: Map[String, String]): ObjectAccessControl = {
    val user = params.get("user")
    val role = params.get("role")

    if (user.isDefined && role.isDefined)
      userToObjectAcl(user.get, role.get)
    else
      throw new IllegalArgumentException("Missing params user and role.")
  }

  def bucketAclFromParams(params: Map[String, String]): BucketAccessControl = {
    val user = params.get("user")
    val role = params.get("role")

    if (user.isDefined && role.isDefined)
      userToBucketAcl(user.get, role.get)
    else
      throw new IllegalArgumentException("Missing params user and role.")
  }



  // User
  def userToBucketAcl(user: String, role: String) =
    new BucketAccessControl().setEntity(user.toGoogleUserEntity).setRole(role).setEmail(user)

  def userToObjectAcl(user: String, role: String) =
    new ObjectAccessControl().setEntity(user.toGoogleUserEntity).setRole(role).setEmail(user)

  //Entity
  def prefixNamespace(entity: AgoraEntity) =
    "agora_" + entity.namespace.get

  def entityToBucket(entity: AgoraEntity) =
    new Bucket().setName(prefixNamespace(entity))

  def entityToObject(entity: AgoraEntity) = {
    new StorageObject()
      .setBucket(prefixNamespace(entity))
      .setName(entity.namespace.get + "_" + entity.name.get + "_" + entity.snapshotId.get)
  }


  // Buckets
  val buckets = client.buckets
  def getBucket(bucket: Bucket) =
    buckets.get(bucket.getName)

  def createBucket(bucket: Bucket) =
    buckets.insert(AgoraConfig.gcsProjectId, bucket)

  def deleteBucket(bucket: Bucket) =
    buckets.delete(bucket.getName)

  def listBuckets(projectName: String = AgoraConfig.gcsProjectId) =
    buckets.list(projectName)

  def patchBucket(bucket: Bucket, content: Bucket) =
    buckets.patch(bucket.getName, content)

  def patchBucketAcls(bucket: Bucket, acls: java.util.List[BucketAccessControl]) =
    patchBucket(bucket, new Bucket().setAcl(acls))


  // Objects
  val objects = client.objects
  def getObject(_object: StorageObject) =
    objects.get(_object.getBucket, _object.getName)

  def createObject(_object: StorageObject) =
    objects.insert(_object.getBucket , _object, ByteArrayContent.fromString("application/json", ""))

  def deleteObject(_object: StorageObject) =
    objects.delete(_object.getBucket, _object.getName)

  def listObjects(bucket: Bucket) =
    objects.list(bucket.getName)

  def patchObject(_object: StorageObject, content: StorageObject) =
    objects.patch(_object.getBucket, _object.getName, content)

  def patchObjectAcls(_object: StorageObject, acls: java.util.List[ObjectAccessControl]) =
    patchObject(_object, new StorageObject().setAcl(acls))


  // Bucket ACLs
  val bucketAcls = client.bucketAccessControls
  def getBucketAcl(bucket: Bucket, user: String) =
    bucketAcls.get(bucket.getName, user.toGoogleUserEntity)

  def createBucketAcl(bucket: Bucket, content: BucketAccessControl) =
    bucketAcls.insert(bucket.getName, content)

  def deleteBucketAcl(bucket: Bucket, user: String) =
    bucketAcls.delete(bucket.getName, user.toGoogleUserEntity)

  def listBucketAcl(bucket: Bucket) =
    bucketAcls.list(bucket.getName)

  def patchBucketAcl(bucket: Bucket, user: String, content: BucketAccessControl) =
    bucketAcls.patch(bucket.getName, user.toGoogleUserEntity, content)


  // Object ACLs
  val objectAcls = client.objectAccessControls
  def getObjectAcl(_object: StorageObject, user: String) =
    objectAcls.get(_object.getBucket, _object.getName, user.toGoogleUserEntity)

  def createObjectAcl(_object: StorageObject, content: ObjectAccessControl) =
    objectAcls.insert(_object.getBucket, _object.getName, content)

  def deleteObjectAcl(_object: StorageObject, user: String) =
    objectAcls.delete(_object.getBucket, _object.getName, user.toGoogleUserEntity)

  def listObjectAcl(_object: StorageObject) =
    objectAcls.list(_object.getBucket, _object.getName)

  def patchObjectAcl(_object: StorageObject, user: String, content: ObjectAccessControl) =
    objectAcls.patch(_object.getBucket, _object.getName, user.toGoogleUserEntity, content)

}

object GcsClientHelpers{
  implicit class StringImplicit(s: String) {
    def toGoogleUserEntity = "user-" + s
    def toGoogleGroupEntity = "group-" + s
  }
}