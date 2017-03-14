package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityNotFoundException, NamespaceAuthorizationException, ValidationException}
import org.broadinstitute.dsde.agora.server.dataaccess.{AgoraDao, WriteAction}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupport, AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import slick.dbio.Effect.Write
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import spray.json._

class AgoraBusiness {

  def insert(agoraEntity: AgoraEntity, username: String): ReadWriteAction[AgoraEntity] = {
    if (!NamespacePermissionsClient.getNamespacePermission(agoraEntity, username).canCreate &&
        !NamespacePermissionsClient.getNamespacePermission(agoraEntity, "public").canCreate) {
      throw new NamespaceAuthorizationException(AgoraPermissions(Create), agoraEntity, username)
    }

    validatePayload(agoraEntity, username)

    val entityToInsert = agoraEntity.entityType.get match {
      case AgoraEntityType.Configuration =>
        val method = resolveMethodRef(agoraEntity.payload.get)
        if (!AgoraEntityPermissionsClient.getEntityPermission(method, username).canRead &&
            !AgoraEntityPermissionsClient.getEntityPermission(method, "public").canRead) {
          throw new AgoraEntityNotFoundException(method)
        }

        agoraEntity.addMethodId(method.id.get.toHexString)
      case _ => agoraEntity
    }

    val entityWithId = AgoraDao.createAgoraDao(entityToInsert.entityType).insert(entityToInsert.addDate())
    val userAccess = new AccessControl(username, AgoraPermissions(All))

    def updateThing(bool: Boolean, entity: AgoraEntity): WriteAction[Unit] = {
      if(bool) {
        NamespacePermissionsClient.addEntity(entity) andThen
          NamespacePermissionsClient.insertNamespacePermission(entityWithId, userAccess)
      } else {
        DBIO.successful(())
      }
    }


    for {
      existsInNamespace <- NamespacePermissionsClient.doesEntityExists(agoraEntity)
      _ <- updateThing(existsInNamespace, entityWithId)
      _ <- AgoraEntityPermissionsClient.addEntity(entityWithId)
      _ <- AgoraEntityPermissionsClient.insertEntityPermission(entityWithId, userAccess)
    } yield {
      entityWithId.addUrl().removeIds()
    }
  }

  def delete(agoraEntity: AgoraEntity, entityTypes: Seq[AgoraEntityType.EntityType], username: String): Int = {
    if (!NamespacePermissionsClient.getNamespacePermission(agoraEntity, username).canRedact &&
        !NamespacePermissionsClient.getNamespacePermission(agoraEntity, "public").canRedact &&
        !NamespacePermissionsClient.isAdmin(username)) {
      throw new NamespaceAuthorizationException(AgoraPermissions(Redact), agoraEntity, username)
    }

    // if the entity was a method, then redact all associated configurations
    if (entityTypes equals AgoraEntityType.MethodTypes) {

      val dao = AgoraDao.createAgoraDao(entityTypes)
      val entityWithId = dao.findSingle(agoraEntity.namespace.get, agoraEntity.name.get, agoraEntity.snapshotId.get)
      val configurations = dao.findConfigurations(entityWithId.id.get)

      configurations.foreach {config => AgoraEntityPermissionsClient.deleteAllPermissions(config)}
    }

    AgoraEntityPermissionsClient.deleteAllPermissions(agoraEntity)
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
    val entities = AgoraEntityPermissionsClient.filterEntityByRead(Seq(foundEntity), username)
    entities match {
      case Seq(ae: AgoraEntity) => ae.addUrl().removeIds().addManagers(AgoraEntityPermissionsClient.listOwners(foundEntity))
      case _ => throw new AgoraEntityNotFoundException(foundEntity)
    }
  }

  def findSingle(entity: AgoraEntity,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): AgoraEntity = {
    findSingle(entity.namespace.get, entity.name.get, entity.snapshotId.get, entityTypes, username)
  }

  private def validatePayload(agoraEntity: AgoraEntity, username: String): Unit = {
    agoraEntity.entityType.get match {

      case AgoraEntityType.Task =>
// GAWB-59 remove wdl validation
//        val namespace = WdlNamespace.load(agoraEntity.payload.get, BackendType.LOCAL)
//        // Passed basic validation.  Now check if (any) docker images that are referenced exist
//        namespace.tasks.foreach { validateDockerImage }

      case AgoraEntityType.Workflow =>
// GAWB-59 remove wdl validation
//        val resolver = MethodImportResolver(username, this)
//        val namespace = WdlNamespace.load(agoraEntity.payload.get, resolver.importResolver _, BackendType.LOCAL)
//        // Passed basic validation.  Now check if (any) docker images that are referenced exist
//        namespace.tasks.foreach { validateDockerImage }

      case AgoraEntityType.Configuration =>
        val json = agoraEntity.payload.get.parseJson
        val fields = json.asJsObject.getFields("methodRepoMethod")
        if(fields.size != 1) throw new ValidationException("Configuration payload must define at least one field named 'methodRepoMethod'.")

        val subFields = fields(0).asJsObject.getFields("methodNamespace", "methodName", "methodVersion")
        if(!subFields(0).isInstanceOf[JsString]) throw new ValidationException("Configuration methodRepoMethod must include a 'methodNamespace' key with a string value")
        if(!subFields(1).isInstanceOf[JsString]) throw new ValidationException("Configuration methodRepoMethod must include a 'methodName' key with a string value")
        if(!subFields(2).isInstanceOf[JsNumber]) throw new ValidationException("Configuration methodRepoMethod must include a 'methodVersion' key with a JSNumber value")
    }
  }

//  private def validateDockerImage(task: Task) = {
//    // Per DSDEEPB-2525, in the interests of expediency, we are disabling docker image validation as we do not support validation of private docker images
//    // When that functionality is added back in, then this is where it should go.
//
////    if (task.runtimeAttributes.docker.isDefined) {
////      val dockerImageReference = parseDockerString(task.runtimeAttributes.docker.get)
////      if (dockerImageReference.isDefined) {
////        DockerHubClient.doesDockerImageExist(dockerImageReference.get)
////      }
////    }
//    true
//  }

  private def resolveMethodRef(payload: String): AgoraEntity = {
    val queryMethod = AgoraApiJsonSupport.methodRef(payload)
    AgoraDao.createAgoraDao(AgoraEntityType.MethodTypes).findSingle(queryMethod)
  }

  /**
   * Parses out user/image:tag from a docker string.
   *
   * @param imageId docker imageId string.  Looks like ubuntu:latest ggrant/joust:latest
   */
//  private def parseDockerString(imageId: String) : Option[DockerImageReference] = {
//    if (imageId.startsWith("gcr.io")) {
//      None
//    } else {
//      val splitUser = imageId.split('/')
//      if (splitUser.length > 2) {
//        throw new SyntaxError("Docker image string '" + imageId + "' is malformed")
//      }
//      val user = if (splitUser.length == 1) None else Option(splitUser(0))
//      val splitTag = splitUser(splitUser.length - 1).split(':')
//      if (splitTag.length > 2) {
//        throw new SyntaxError("Docker image string '" + imageId + "' is malformed")
//      }
//      val repo = splitTag(0)
//      val tag = if (splitTag.length == 1) "latest" else splitTag(1)
//      Option(DockerImageReference(user, repo, tag))
//    }
//  }
}