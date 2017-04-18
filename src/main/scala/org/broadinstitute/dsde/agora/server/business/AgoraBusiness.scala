package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.exceptions.{AgoraEntityNotFoundException, NamespaceAuthorizationException, ValidationException}
import org.broadinstitute.dsde.agora.server.dataaccess.{AgoraDao, ReadWriteAction}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model.{AgoraApiJsonSupport, AgoraEntity, AgoraEntityProjection, AgoraEntityType}
import slick.dbio.DBIO
import spray.json._
import wdl4s.{AstTools, WdlNamespace, WdlNamespaceWithWorkflow}
import wdl4s.parser.WdlParser.{AstList, AstNode, SyntaxError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class AgoraBusiness(permissionsDataSource: PermissionsDataSource)(implicit ec: ExecutionContext) {

  //Creates a fake extra permission with the desired permission level conditional on checkAdmin and the user being an admin
  private def makeDummyAdminPermission(checkAdmin: Boolean, permClient: PermissionsClient, username: String, permLevel: AgoraPermissions) = {
    if(checkAdmin) {
      permClient.isAdmin(username) map {
        case true => permLevel
        case false => AgoraPermissions(Nothing)
      }
    } else {
      DBIO.successful(AgoraPermissions(Nothing))
    }
  }

  private def checkNamespacePermission[T](db: DataAccess, agoraEntity: AgoraEntity, username: String, permLevel: AgoraPermissions, checkAdmin: Boolean = false)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    //if we're supposed to check if the user is an admin, create a fake action
    val admAction = makeDummyAdminPermission(checkAdmin, db.nsPerms, username, permLevel)

    DBIO.sequence(Seq(db.nsPerms.getNamespacePermission(agoraEntity, username), db.nsPerms.getNamespacePermission(agoraEntity, "public"), admAction)) flatMap { namespacePerms =>
      db.nsPerms.isAdmin(username)
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
    db.nsPerms.doesEntityExists(entity) flatMap { exists =>
      if(!exists) {
        db.nsPerms.addEntity(entity) flatMap { _ =>
          db.nsPerms.insertNamespacePermission(entityWithId, userAccess) map { _ => () }
        }
      } else {
        DBIO.successful(())
      }
    }
  }

  //Workaround for https://github.com/broadinstitute/wdl4s/issues/103
  //Checks to see if the WDL has imports, since loading a WDL that contains imports without specifying a resolver
  //barfs in an unintuitive way.
  private def checkWdlHasNoImports(wdl: String): Try[Unit] = {
    Try { //AST loader might syntax error if the wdl is crap
      val ast = AstTools.getAst(wdl, "string")
      ast.getAttribute("imports").asInstanceOf[AstList].isEmpty
    } match {
      case Failure(x) => Failure(x) //ast didn't parse nicely
      case Success(true) => Success() //no imports, WDL is fine
      case Success(false) => Failure(ValidationException("WDL imports not yet supported."))
    }
  }

  private def checkValidPayload[T](agoraEntity: AgoraEntity, username: String)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    val payload = agoraEntity.payload.get

    val payloadOK = agoraEntity.entityType match {
        case Some(AgoraEntityType.Task) =>
          checkWdlHasNoImports(payload) flatMap ( _ => WdlNamespace.loadUsingSource(payload, None, Option(Seq())) )
        // NOTE: Still not validating existence of docker images.
        // namespace.tasks.foreach { validateDockerImage }

        case Some(AgoraEntityType.Workflow) =>
          checkWdlHasNoImports(payload) flatMap ( _ => WdlNamespaceWithWorkflow.load(payload, Seq()) )
        // NOTE: Still not validating existence of docker images.
        //namespace.tasks.foreach { validateDockerImage }

        case Some(AgoraEntityType.Configuration) =>
          Try {
            val json = payload.parseJson
            val fields = json.asJsObject.getFields("methodRepoMethod")
            if (fields.size != 1) throw ValidationException("Configuration payload must define at least one field named 'methodRepoMethod'.")

            val subFields = fields.head.asJsObject.getFields("methodNamespace", "methodName", "methodVersion")
            if (!subFields(0).isInstanceOf[JsString]) throw ValidationException("Configuration methodRepoMethod must include a 'methodNamespace' key with a string value")
            if (!subFields(1).isInstanceOf[JsString]) throw ValidationException("Configuration methodRepoMethod must include a 'methodName' key with a string value")
            if (!subFields(2).isInstanceOf[JsNumber]) throw ValidationException("Configuration methodRepoMethod must include a 'methodVersion' key with a JSNumber value")
          }

        case None => //hello "shouldn't get here" my old friend
          Failure(ValidationException(s"AgoraEntity $agoraEntity has no type!"))
      }

    payloadOK match {
      case Success(_) => op
      case Failure(e: SyntaxError) =>
        DBIO.failed(ValidationException(e.getMessage, e.getCause))
      case Failure(regret) =>
        DBIO.failed(regret)
    }
  }

  def insert(agoraEntity: AgoraEntity, username: String): Future[AgoraEntity] = {
    //this goes to Mongo, so do this outside the permissions txn
    val configReferencedMethodOpt = agoraEntity.entityType.get match {
      case AgoraEntityType.Configuration => Some(resolveMethodRef(agoraEntity.payload.get))
      case _ => None
    }

    permissionsDataSource.inTransaction { db =>
      db.aePerms.addUserIfNotInDatabase(username) flatMap { _ =>
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
      db.aePerms.addUserIfNotInDatabase(username) flatMap { _ =>
        checkNamespacePermission(db, agoraEntity, username, AgoraPermissions(Redact), checkAdmin = true) {
          //admins can redact anything
          DBIO.sequence(configurations map { config => db.aePerms.deleteAllPermissions(config) }) andThen
            db.aePerms.deleteAllPermissions(agoraEntity)
        }
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
      for {
        _ <- db.aePerms.addUserIfNotInDatabase(username)
        entity <- db.aePerms.filterEntityByRead(entities, username)
      } yield entity
    }
  }

  def findSingle(namespace: String,
                 name: String,
                 snapshotId: Int,
                 entityTypes: Seq[AgoraEntityType.EntityType],
                 username: String): Future[AgoraEntity] = {
    val foundEntity = AgoraDao.createAgoraDao(entityTypes).findSingle(namespace, name, snapshotId)

    permissionsDataSource.inTransaction { db =>
      db.aePerms.addUserIfNotInDatabase(username) flatMap { _ =>
        db.aePerms.filterEntityByRead(Seq(foundEntity), username) flatMap {
          case Seq(ae: AgoraEntity) => db.aePerms.listOwners(foundEntity) map { owners => ae.addUrl().removeIds().addManagers(owners) }
          case _ => DBIO.failed(AgoraEntityNotFoundException(foundEntity))
        }
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