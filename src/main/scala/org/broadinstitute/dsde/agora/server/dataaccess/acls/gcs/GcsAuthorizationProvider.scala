
package org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs

import akka.actor._
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.acls._
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model._
import org.broadinstitute.dsde.agora.server.webservice.util.GoogleCredentialHandler
import spray.client.pipelining._
import spray.http.{HttpResponse, OAuth2BearerToken}
import spray.http.StatusCodes._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}

object GcsAuthorizationProvider extends AuthorizationProvider {

  import GcsAuthHelpers._
  import GcsUtil._
  import actorSystem.dispatcher

  override def namespaceAuthorization(entity: AgoraEntity, username: String): AgoraPermissions = {
    val isBucketFound = doesBucketExist(entity, username)

    if (!isBucketFound) new AgoraPermissions(Create)
    else getBucketPermissions(entity, username)
  }

  override def entityAuthorization(entity: AgoraEntity, username: String): AgoraPermissions = {
    val url = objectAclUrl(entity, username)
    val objectAcl = gcsGET(url)

    val permissions = objectAcl.map { acl =>
      val role = GcsObjectRole(unrollGoogleAcl(acl))
      RoleTranslator.gcsObjectToEntityPermissions(role)
    }
    Await.result(permissions, timeout)
  }

  override def createEntityAuthorizations(entity: AgoraEntity, username: String): Unit = {
    val bucket = Promise[HttpResponse]()
    val bucketAcl = Promise[HttpResponse]()
    val _object = Promise[HttpResponse]()
    val objectAcl = Promise[HttpResponse]()

    createBucket(entity, username, bucket)
    bucket.future.onSuccess { case _ => addUserAclToBucket(entity, username, bucketAcl) }
    bucketAcl.future.onSuccess { case _ => createObject(entity, username, _object) }
    _object.future.onSuccess { case _ => addUserAclToObject(entity, username, objectAcl) }

    val finalResult = Await.result(objectAcl.future, timeout)
    if(finalResult.status != OK) throw new GoogleConnectionException()
  }
}

object GcsUtil {

  import GcsAuthHelpers._
  import actorSystem.dispatcher

  case class GoogleConnectionException(text: String = "There was a problem connecting to Google Cloud Storage.") extends Exception(text)

  def doesBucketExist(entity: AgoraEntity, username: String): Boolean = {
    val url = bucketUrl(entity)
    val bucket = gcsGET(url)

    val bucketNotFound = bucket map { r: HttpResponse => r.status == NotFound }

    bucket.onFailure { case _ => throw GoogleConnectionException() }

    val isBucketFound = !Await.result(bucketNotFound, timeout)
    isBucketFound
  }

  def getBucketPermissions(entity: AgoraEntity, username: String): AgoraPermissions = {
    val url = bucketAclUrl(entity, username)
    val bucketAcl = gcsGET(url)

    val permissions = bucketAcl map { acl: HttpResponse =>
      val role = GcsBucketRole(unrollGoogleAcl(acl))
      RoleTranslator.gcsBucketToNamespacePermissions(role)
    }

    bucketAcl.onFailure { case _ => throw GoogleConnectionException() }

    Await.result(permissions, timeout)
  }

  def createBucket(entity: AgoraEntity, username: String, bucket: Promise[HttpResponse]): Unit = {
    val url = createBucketUrl
    val content = BucketResource(name = Option(bucketName(entity)))
    val response = gcsPOST(url, content)

    response.onSuccess { case r: HttpResponse =>
      if (r.status == OK || r.status == Conflict) bucket.success(r)
      else throw GoogleConnectionException(text = r.entity.asString)
    }
    response.onFailure { case _ => throw GoogleConnectionException() }
  }

  def addUserAclToBucket(entity: AgoraEntity, username: String, bucketAcl: Promise[HttpResponse]): Unit = {
    val url = createBucketAclUrl(entity)
    val content = GoogleAccessControls(kind = Option(GoogleAccessControls.bucketKind),
      entity = Option(prefixUser(username)),
      role = Option(GcsRole.Owner))
    val response = gcsPOST(url, content)

    response.onSuccess { case r: HttpResponse =>
      if (r.status == OK) bucketAcl.success(r)
      else throw GoogleConnectionException(text = r.entity.asString)
    }
    response.onFailure { case _ => throw GoogleConnectionException() }
  }

  def createObject(entity: AgoraEntity, username: String, _object: Promise[HttpResponse]): Unit = {
    val url = createObjectUrl(entity)
    val response = gcsPOST(url)

    response.onSuccess { case r: HttpResponse =>
      if (r.status == OK) _object.success(r)
      else throw GoogleConnectionException(text = r.entity.asString)
    }
    response.onFailure { case _ => throw GoogleConnectionException() }
  }

  def addUserAclToObject(entity: AgoraEntity, username: String, objectAcl: Promise[HttpResponse]): Unit = {
    val url = createObjectAclUrl(entity)
    val content = GoogleAccessControls(kind = Option(GoogleAccessControls.objectKind),
      entity = Option(prefixUser(username)),
      role = Option(GcsRole.Owner))
    val response = gcsPOST(url, content)

    response.onSuccess { case r: HttpResponse =>
      if (r.status == OK) objectAcl.success(r)
      else objectAcl.failure(new GoogleConnectionException(text = r.entity.asString))
    }
    response.onFailure { case t: Throwable => throw GoogleConnectionException() }
  }

  //ACL Response Handler
  def unrollGoogleAcl(response: HttpResponse): String = {
    response.status match {
      case OK => unmarshal[GoogleAccessControls].apply(response).role.get // READER or OWNER
      case NotFound => GcsRole.Nothing
      case Forbidden => GcsRole.Nothing
      case _ => throw GoogleConnectionException(text = response.entity.asString)
    }
  }
}

object GcsAuthHelpers extends LazyLogging {
  implicit val actorSystem = ActorSystem("agora")

  import actorSystem.dispatcher

  val timeout = 5.seconds

  // String Conversion helpers
  def bucketName(e: AgoraEntity) = "agoranamespace_" + e.namespace.get.toLowerCase

  def objectName(e: AgoraEntity) = "agoraEntity_" + e.entityType.get + "_" + e.name.get + "_" + e.snapshotId.get

  def prefixUser(userEmail: String): String = "user-" + userEmail

  // URL helpers
  val gcsBaseUrl = "https://www.googleapis.com/storage/v1/b/"
  val gcsUploadUrl = "https://www.googleapis.com/upload/storage/v1/b/"

  def bucketUrl(e: AgoraEntity) = gcsBaseUrl + bucketName(e)

  def bucketAclUrl(e: AgoraEntity, u: String) = gcsBaseUrl + bucketName(e) + "/acl/" + prefixUser(u)

  def objectAclUrl(e: AgoraEntity, u: String) = gcsBaseUrl + bucketName(e) + "/o/" + objectName(e) + "/acl/" + prefixUser(u)

  val createBucketUrl = gcsBaseUrl + "?project=" + AgoraConfig.gcsProjectId

  def createBucketAclUrl(e: AgoraEntity) = gcsBaseUrl + bucketName(e) + "/acl"

  def createObjectUrl(e: AgoraEntity) = gcsUploadUrl + bucketName(e) + "/o" + "?uploadType=media" + "&name=" + objectName(e)

  def createObjectAclUrl(e: AgoraEntity) = gcsBaseUrl + bucketName(e) + "/o/" + objectName(e) + "/acl"

  // GCS sends back a malformed ETag header according to HTTP standards.
  // Spray complains. We currently don't care about ETag, so filter it out.
  val removeEtagHeaders = { r: HttpResponse => r.withHeaders(r.headers.filter(!_.name.startsWith("ETag"))) }

  def token = OAuth2BearerToken(GoogleCredentialHandler.accessToken)

  def pipeline = addCredentials(token) ~> sendReceive ~> removeEtagHeaders

  def gcsGET(url: String): Future[HttpResponse] = pipeline(Get(url))

  def gcsPOST(url: String): Future[HttpResponse] = pipeline(Post(url))

  def gcsPOST(url: String, content: GoogleAccessControls): Future[HttpResponse] = pipeline(Post(url, content))

  def gcsPOST(url: String, content: BucketResource): Future[HttpResponse] = pipeline(Post(url, content))

}


