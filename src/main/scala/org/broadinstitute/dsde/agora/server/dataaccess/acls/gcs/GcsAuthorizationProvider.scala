
package org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs

import akka.actor._
import scala.concurrent.Future
import com.typesafe.scalalogging.slf4j.LazyLogging
import org.broadinstitute.dsde.agora.server.{AgoraConfig, GoogleCredentialHandler}
import org.broadinstitute.dsde.agora.server.dataaccess.acls._
import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.{GoogleAccessControls, AgoraEntity}
import GcsApiJsonSupport._
import spray.client.pipelining._
import spray.http.OAuth2BearerToken
import spray.http.StatusCodes._
import spray.http.HttpResponse
import scala.concurrent.duration._

import scala.concurrent.Await

object GcsAuthorizationProvider extends AuthorizationProvider with LazyLogging {
  implicit val actorSystem = ActorSystem("agora")
  import actorSystem.dispatcher

  val timeout = 20 seconds

  val gcsBaseUrl = "https://www.googleapis.com/storage/v1/b/"

  // GCS sends back a malformed ETag header according to HTTP standards. Spray complains. We currently don't care about ETag, so filter it out.
  val removeEtagHeaders: HttpResponse => HttpResponse =
    r => r.withHeaders(r.headers.filter(!_.name.startsWith("ETag")))

  val pipeline = addCredentials(OAuth2BearerToken(GoogleCredentialHandler.accessToken)) ~> sendReceive ~> removeEtagHeaders

  private case class GoogleResponse(entity: AgoraEntity, response: HttpResponse)

  def namespaceToBucketName(namespace: String): String = "agoranamespace_" + namespace.toLowerCase

  def entityToObjectName(entity: AgoraEntity): String = "agoraEntity_" + entity.entityType.get + "_" + entity.name.get + "_" + entity.snapshotId.get

  def getGcsObjectRoleRequestUrl(agoraEntity: AgoraEntity, userEmail: String): String = {
    val agoraBusiness = new AgoraBusiness
    agoraBusiness.hasNamespaceNameId(agoraEntity) match {
      case true =>
        gcsBaseUrl + namespaceToBucketName(agoraEntity.namespace.get) + "/o/" + entityToObjectName(agoraEntity) + "/acl/user-" + userEmail
      case false =>
        throw new Exception("Trying to get Google ACLs for AgoraEntity with missing namespace, name, or snapshotId")
    }
  }

  def getGcsBucketRoleRequestUrl(namespace: String, userEmail: String): String = gcsBaseUrl + namespaceToBucketName(namespace) + "/acl/user-" + userEmail

  def insertGoogleBucketUrl(): String = gcsBaseUrl + AgoraConfig.gcsProjectId

  def unrollGaclResponse(response:HttpResponse): GcsObjectRole = {
    response.status match {
      case OK =>
        val GaclResponse = unmarshal[GoogleAccessControls].apply(response)
        GcsObjectRole(GaclResponse.role.get) // String. READER or OWNER
      case NotFound => GcsObjectRole(GcsRole.Nothing) // No ACL means the user has Nothing permission
      case _ =>
        // 500 and other errors.
        logger.error("Error contacting GCS for object ACLs: " + response.status.toString() + response.entity.asString)
        // Need to catch this error in the calling handler, so that requestContext can be completed
        throw new ClientServiceFailure(response.status, response.entity.asString)
    }
  }

  override def authorizationsForEntity(agoraEntity: Option[AgoraEntity], username: String): AuthorizedAgoraEntity = {
    agoraEntity match {
      case None =>  AuthorizedAgoraEntity(None, new AgoraPermissions(Nothing))
      case Some(entity) =>
        val responseFuture: Future[AuthorizedAgoraEntity] =
          pipeline(Get(getGcsObjectRoleRequestUrl(entity, username))).map(response =>
            AuthorizedAgoraEntity(agoraEntity, RoleTranslator.gcsObjectToEntityPermissions(unrollGaclResponse(response))))
        Await.result(responseFuture, timeout)
    }
  }

  override def authorizationsForEntities(agoraEntities: Seq[AgoraEntity], username: String): Seq[AuthorizedAgoraEntity] = {
    // Get a sequence of response Futures, one for each Some(entity). Transform the entity + HttpResponse -> AuthorizedAgoraEntity
    // Transform the sequence of Futures into a Future of a sequence
    // Futures can complete in parallel. We transform them as they complete. Overall, we await the result on the slowest HttpResponse
    val responseFutures: Future[Seq[AuthorizedAgoraEntity]] = Future.sequence(agoraEntities.map(entity => {
      pipeline(Get(getGcsObjectRoleRequestUrl(entity, username))).map(httpResponse =>
        AuthorizedAgoraEntity(Option(entity), RoleTranslator.gcsObjectToEntityPermissions(unrollGaclResponse(httpResponse))))
      }))
    Await.result(responseFutures, timeout)
  }
}


