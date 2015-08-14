package org.broadinstitute.dsde.agora.server.business

import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.webservice.util.{DockerImageReference, DockerHubClient}

import cromwell.binding._
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraPermissions, AuthorizationProvider}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityProjection, AgoraEntityType}

class AgoraBusiness(authorizationProvider: AuthorizationProvider) {

  def insert(agoraEntity: AgoraEntity, username: String): AgoraEntity = {
    if (authorizationProvider.isAuthorizedForCreation(agoraEntity, username)) {
      validatePayload(agoraEntity, username)

      val entityWithId = AgoraDao.createAgoraDao(agoraEntity.entityType).insert(agoraEntity.addDate())
      authorizationProvider.createEntityAuthorizations(entityWithId, username)
      entityWithId.addUrl()

    } else {
      throw new EntityAuthorizationException(AgoraPermissions(Create), agoraEntity)
    }
  }

  def find(agoraSearch: AgoraEntity,
           agoraProjection: Option[AgoraEntityProjection],
           entityTypes: Seq[AgoraEntityType.EntityType],
           username: String): Seq[AgoraEntity] = {
    val entities = AgoraDao.createAgoraDao(entityTypes)
      .find(agoraSearch, agoraProjection)
      .map(entity => entity.addUrl())

    authorizationProvider.filterByReadPermissions(entities, username)
  }

  def findSingle(namespace: String,
                 name: String,
                 snapshotId: Int,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): AgoraEntity = {
    val foundEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId)

    if (authorizationProvider.isAuthorizedForRead(foundEntity, username))
      foundEntity.addUrl()
    else
      throw new EntityAuthorizationException(AgoraPermissions(Read), foundEntity)
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
        val resolver = MethodImportResolver(username, this, authorizationProvider)
        val namespace = WdlNamespace.load(agoraEntity.payload.get, resolver.importResolver _)
        // Passed basic validation.  Now check if (any) docker images that are referenced exist
        namespace.tasks.foreach { validateDockerImage }
      case AgoraEntityType.Configuration =>
      //add config validation here
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