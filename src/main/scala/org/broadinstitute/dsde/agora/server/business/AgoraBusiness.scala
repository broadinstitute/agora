package org.broadinstitute.dsde.agora.server.business

import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.webservice.util.{DockerImageReference, DockerHubClient}

import cromwell.binding._
import org.broadinstitute.dsde.agora.server.dataaccess.{AgoraEntityNotFoundException, AgoraDao}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.NamespacePermissionsClient
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupport, AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import spray.json
import spray.json._
import DefaultJsonProtocol._

class AgoraBusiness {

  def insert(agoraEntity: AgoraEntity, username: String): AgoraEntity = {
    if (NamespacePermissionsClient.getNamespacePermission(agoraEntity, username).canCreate) {
      validatePayload(agoraEntity, username)

      val entityToInsert = agoraEntity.entityType.get match {
        case AgoraEntityType.Configuration =>
          val method = resolveMethodRef(agoraEntity.payload.get)
          if (!AgoraEntityPermissionsClient.getEntityPermission(method, username).canRead) {
            throw new AgoraEntityNotFoundException(method)
          }
          agoraEntity.addMethodId(method.id.get.toHexString)
        case _ => agoraEntity
      }
      val entityWithId = AgoraDao.createAgoraDao(entityToInsert.entityType).insert(entityToInsert.addDate())
      val userAccess = new AccessControl(username, AgoraPermissions(All))

      if (!NamespacePermissionsClient.doesEntityExists(agoraEntity)) {
        NamespacePermissionsClient.addEntity(entityWithId)
        NamespacePermissionsClient.insertNamespacePermission(entityWithId, userAccess)
      }

      AgoraEntityPermissionsClient.addEntity(entityWithId)
      AgoraEntityPermissionsClient.insertEntityPermission(entityWithId, userAccess)
      entityWithId.addUrl().removeIds()
    } else {
      throw new NamespaceAuthorizationException(AgoraPermissions(Create), agoraEntity, username)
    }
  }

  def find(agoraSearch: AgoraEntity,
           agoraProjection: Option[AgoraEntityProjection],
           entityTypes: Seq[AgoraEntityType.EntityType],
           username: String): Seq[AgoraEntity] = {

    val entities = AgoraDao.createAgoraDao(entityTypes)
      .find(agoraSearch, agoraProjection)
      .map(entity => entity.addUrl().removeIds())

    AgoraEntityPermissionsClient.filterEntityByRead(entities, username)
  }

  def findSingle(namespace: String,
                 name: String,
                 snapshotId: Int,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): AgoraEntity = {
    val foundEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId)

    if (AgoraEntityPermissionsClient.getEntityPermission(foundEntity, username).canRead)
      foundEntity.addUrl().removeIds()
    else
      throw new EntityAuthorizationException(AgoraPermissions(Read), foundEntity, username)
  }

  def findSingle(entity: AgoraEntity, entityTypes: Seq[AgoraEntityType.EntityType], username: String): AgoraEntity = {
    findSingle(entity.namespace.get, entity.name.get, entity.snapshotId.get, entityTypes, username)
  }

  private def validatePayload(agoraEntity: AgoraEntity, username: String): Unit = {
    agoraEntity.entityType.get match {
      case AgoraEntityType.Task =>
        val namespace = WdlNamespace.load(agoraEntity.payload.get)
        // Passed basic validation.  Now check if (any) docker images that are referenced exist
        namespace.tasks.foreach { validateDockerImage }
      case AgoraEntityType.Workflow =>
        val resolver = MethodImportResolver(username, this)
        val namespace = WdlNamespace.load(agoraEntity.payload.get, resolver.importResolver _)
        // Passed basic validation.  Now check if (any) docker images that are referenced exist
        namespace.tasks.foreach { validateDockerImage }
      case AgoraEntityType.Configuration =>
        val json = agoraEntity.payload.get.parseJson
        val fields = json.asJsObject.getFields("methodStoreMethod")
        require(fields.size == 1)
        val subFields = fields(0).asJsObject.getFields("methodNamespace", "methodName", "methodVersion")
        require(subFields(0).isInstanceOf[JsString])
        require(subFields(1).isInstanceOf[JsString])
        require(subFields(2).isInstanceOf[JsNumber])
    }
  }

  private def validateDockerImage(task: Task) = {
    if (task.runtimeAttributes.docker.isDefined) {
      val dockerImageReference = parseDockerString(task.runtimeAttributes.docker.get)
      if (dockerImageReference.isDefined) {
        DockerHubClient.doesDockerImageExist(dockerImageReference.get)
      }
    }
  }

  private def resolveMethodRef(payload: String): AgoraEntity = {
    val queryMethod = AgoraApiJsonSupport.methodRef(payload)
    AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(queryMethod)
  }

  /**
   * Parses out user/image:tag from a docker string.
   *
   * @param imageId docker imageId string.  Looks like ubuntu:latest ggrant/joust:latest
   */
  private def parseDockerString(imageId: String) : Option[DockerImageReference] = {
    if (imageId.startsWith("gcr.io")) {
      None
    } else {
      val splitUser = imageId.split('/')
      if (splitUser.length > 2) {
        throw new SyntaxError("Docker image string '" + imageId + "' is malformed")
      }
      val user = if (splitUser.length == 1) None else Option(splitUser(0))
      val splitTag = splitUser(splitUser.length - 1).split(':')
      if (splitTag.length > 2) {
        throw new SyntaxError("Docker image string '" + imageId + "' is malformed")
      }
      val repo = splitTag(0)
      val tag = if (splitTag.length == 1) "latest" else splitTag(1)
      Option(DockerImageReference(user, repo, tag))
    }
  }
}
