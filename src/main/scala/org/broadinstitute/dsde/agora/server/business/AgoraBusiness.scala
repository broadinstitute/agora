package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityNotFoundException, NamespaceAuthorizationException, ValidationException}
import org.broadinstitute.dsde.agora.server.dataaccess.{AgoraDao, ReadWriteAction, WriteAction}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{entities, _}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupport, AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import slick.dbio.Effect.{Read, Write}
import slick.dbio.{DBIO, DBIOAction, Effect, NoStream}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AgoraBusiness(permissionsDataSource: PermissionsDataSource)(implicit ec: ExecutionContext) {

  private def checkNamespacePermission[T](db: DataAccess, agoraEntity: AgoraEntity, username: String, permLevel: AgoraPermissions)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    DBIO.sequence(Seq(db.nsPerms.getNamespacePermission(agoraEntity, username), db.nsPerms.getNamespacePermission(agoraEntity, "public"))) flatMap { namespacePerms =>
      if (!namespacePerms.exists(_.hasPermission(permLevel))) {
        DBIO.failed(NamespaceAuthorizationException(permLevel, agoraEntity, username))
      } else {
        op
      }
    }
  }

  private def checkEntityPermission[T](db: DataAccess, agoraEntity: AgoraEntity, username: String, permLevel: AgoraPermissions)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    DBIO.sequence(Seq(db.aePerms.getEntityPermission(agoraEntity, username), db.aePerms.getEntityPermission(agoraEntity, "public"))) flatMap { entityPerms =>
      if (!entityPerms.exists(_.hasPermission(permLevel))) {
        DBIO.failed(AgoraEntityNotFoundException(agoraEntity))
      } else {
        op
      }
    }
  }

  private def createNamespaceIfNonexistent(db: DataAccess, entity: AgoraEntity, entityWithId: AgoraEntity, userAccess: AccessControl): ReadWriteAction[Unit] = {
    db.nsPerms.doesEntityExists(entity) map { exists =>
      if(exists) {
        db.nsPerms.addEntity(entity) andThen
          db.nsPerms.insertNamespacePermission(entityWithId, userAccess)
      } else {
        DBIO.successful(())
      }
    }
  }

  private def checkValidPayload[T](agoraEntity: AgoraEntity, username: String)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    Try(agoraEntity.entityType.get match {

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
    }) match {
      case Success(_) => op
      case Failure(regret) => DBIO.failed(regret)
    }
  }

  def insert(agoraEntity: AgoraEntity, username: String): Future[AgoraEntity] = {
    //this goes to Mongo, so do this outside the permissions txn
    val configReferencedMethodOpt = agoraEntity.entityType.get match {
      case AgoraEntityType.Configuration => Some(resolveMethodRef(agoraEntity.payload.get))
      case _ => None
    }

    permissionsDataSource.inTransaction { db =>
      checkNamespacePermission(db, agoraEntity, username, AgoraPermissions(Create)) {
        checkValidPayload(agoraEntity, username) {

          //this silliness required to check whether the referenced method is readable if it's a config
          val entityToInsertAction = configReferencedMethodOpt match {
            case Some(referencedMethod) =>
              checkEntityPermission(db, referencedMethod, username, AgoraPermissions(Read)) {
                DBIO.successful(agoraEntity.addMethodId(referencedMethod.id.get.toHexString))
              }
            case None => DBIO.successful(agoraEntity)
          }

          entityToInsertAction flatMap { entityToInsert =>
            //this goes to mongo inside a SQL transaction :(
            val entityWithId = AgoraDao.createAgoraDao(entityToInsert.entityType).insert(entityToInsert.addDate())
            val userAccess = new AccessControl(username, AgoraPermissions(All))

            for {
              _ <- createNamespaceIfNonexistent(db, agoraEntity, entityWithId, userAccess)
              _ <- db.aePerms.addEntity(entityWithId)
              _ <- db.aePerms.insertEntityPermission(entityWithId, userAccess)
            } yield {
              entityWithId.addUrl().removeIds()
            }
          }
        }
      }
    }
  }

  def delete(agoraEntity: AgoraEntity, entityTypes: Seq[AgoraEntityType.EntityType], username: String): Future[Int] = {
    //list of associated configurations. Goes to Mongo so we do it outside the sql txn
    val configurations = if (entityTypes equals AgoraEntityType.MethodTypes) {
      val dao = AgoraDao.createAgoraDao(entityTypes)
      val entityWithId = dao.findSingle(agoraEntity.namespace.get, agoraEntity.name.get, agoraEntity.snapshotId.get)
      dao.findConfigurations(entityWithId.id.get)
    } else {
      Seq()
    }

    permissionsDataSource.inTransaction { db =>
      checkNamespacePermission(db, agoraEntity, username, AgoraPermissions(Redact)) {
        DBIO.sequence(configurations map { config => db.aePerms.deleteAllPermissions(config) }) andThen
          db.aePerms.deleteAllPermissions(agoraEntity)
      }
    }
  }

  def find(agoraSearch: AgoraEntity,
           agoraProjection: Option[AgoraEntityProjection],
           entityTypes: Seq[AgoraEntityType.EntityType],
           username: String): Future[Seq[AgoraEntity]] = {

    val entities = AgoraDao.createAgoraDao(entityTypes)
      .find(agoraSearch, agoraProjection)
      .map(entity => entity.addUrl().removeIds())

    permissionsDataSource.inTransaction { db =>
      db.aePerms.filterEntityByRead(entities, username)
    }
  }

  def findSingle(namespace: String,
                 name: String,
                 snapshotId: Int,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): Future[AgoraEntity] = {
    val foundEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId)

    permissionsDataSource.inTransaction { db =>
      db.aePerms.filterEntityByRead(Seq(foundEntity), username) flatMap {
        case Seq(ae: AgoraEntity) => db.aePerms.listOwners(foundEntity) map { owners => ae.addUrl().removeIds().addManagers(owners) }
        case _ => DBIO.failed(AgoraEntityNotFoundException(foundEntity))
      }
    }
  }

  def findSingle(entity: AgoraEntity,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): Future[AgoraEntity] = {
    findSingle(entity.namespace.get, entity.name.get, entity.snapshotId.get, entityTypes, username)
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