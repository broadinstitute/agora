package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.exceptions._
import org.broadinstitute.dsde.agora.server.dataaccess.{AgoraDao, ReadWriteAction}
import org.broadinstitute.dsde.agora.server.dataaccess.permissions._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraPermissions._
import org.broadinstitute.dsde.agora.server.model._
import org.bson.types.ObjectId
import slick.dbio.DBIO
import spray.http.StatusCodes
import spray.json._
import wdl4s.{AstTools, WdlNamespace, WdlNamespaceWithWorkflow}
import wdl4s.parser.WdlParser.{AstList, AstNode, SyntaxError}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object AgoraBusiness {
  val nameRegex = """[a-zA-Z0-9-_.]+""".r // Applies to entity names & namespaces
  val configIdsProjection = Some(new AgoraEntityProjection(Seq[String]("id", "methodId"), Seq.empty[String]))
}

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

  private def checkInsertPermission[T](db: DataAccess, agoraEntity: AgoraEntity, username: String, snapshots: Seq[AgoraEntity])(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    // if previous snapshots exist, check ownership on them; if not, check permission on the namespace
    if (snapshots.isEmpty)
      checkNamespacePermission(db, agoraEntity, username, AgoraPermissions(Create))(op)
    else {
      val ownerPerm = AgoraPermissions(Create)
      DBIO.sequence(snapshots map { db.aePerms.listEntityPermissions }) flatMap { snapshotPermissions =>
        // ignore redacted snapshots. we have to use listEntityPermissions to check that not only does the
        // current user not have permissions, but nobody has permissions.
        snapshotPermissions.filter(_.nonEmpty) match {
          case x if x.isEmpty =>
            // if all snapshots are redacted, defer to the namespace permissions
            checkNamespacePermission(db, agoraEntity, username, AgoraPermissions(Create))(op)
          case y if !y.flatten.exists(acl => acl.user == username && acl.roles.hasPermission(ownerPerm)) =>
            // if ACLs exist, but current user doesn't have permission, fail
            DBIO.failed(AgoraEntityAuthorizationException(ownerPerm, agoraEntity, username))
          case _ =>
            op
        }
      }
    }
  }

  // as of this writing, no caller specifies checkAdmin=true, but we leave the option here for future use
  private def checkNamespacePermission[T](db: DataAccess, agoraEntity: AgoraEntity, username: String, permLevel: AgoraPermissions, checkAdmin: Boolean = false)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    //if we're supposed to check if the user is an admin, create a fake action
    val admAction = makeDummyAdminPermission(checkAdmin, db.nsPerms, username, permLevel)

    DBIO.sequence(Seq(db.nsPerms.getNamespacePermission(agoraEntity, username), db.nsPerms.getNamespacePermission(agoraEntity, "public"), admAction)) flatMap { namespacePerms =>
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
        // if the user can't even read the entity, throw NotFound for security.
        // if the user can read, but doesn't have the requested permission, throw Forbidden.
        val exceptionToThrow = if (!entityPerms.exists(_.hasPermission(AgoraPermissions(Read)))) {
          AgoraEntityNotFoundException(agoraEntity)
        } else {
          AgoraEntityAuthorizationException(permLevel, agoraEntity, username)
        }
        DBIO.failed(exceptionToThrow)
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

        case _ => //hello "shouldn't get here" my old friend
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

  // Name & namespace for new methods are validated against the regex.
  // Existing methods get a pass because cleaning up the repo is prohibitively difficult (GAWB-1614)
  private def validateNamesForNewEntity[T](entity: AgoraEntity)(op: => ReadWriteAction[T]): ReadWriteAction[T] = {
    (entity.namespace, entity.name) match {
      case (Some(AgoraBusiness.nameRegex(_*)), Some(AgoraBusiness.nameRegex(_*))) => op
      case _ => throw ValidationException(
        "Entity must have both namespace and name and may only contain letters, numbers, underscores, dashes, and periods."
      )
    }
  }

  def insert(agoraEntity: AgoraEntity, username: String): Future[AgoraEntity] = {
    //these next two go to Mongo, so do them outside the permissions txn
    val configReferencedMethodOpt = agoraEntity.entityType.get match {
      case AgoraEntityType.Configuration => Some(resolveMethodRef(agoraEntity.payload.get))
      case _ => None
    }
    // find all previous snapshots. Use the dao directly, because we want to find
    // everything, including those snapshots the current user can't read
    val snapshots = AgoraDao.createAgoraDao(agoraEntity.entityType)
      .find(AgoraEntity(agoraEntity.namespace, agoraEntity.name), None)

    permissionsDataSource.inTransaction { db =>
      db.aePerms.addUserIfNotInDatabase(username) flatMap { _ =>
        checkInsertPermission(db, agoraEntity, username, snapshots) {
          validateNamesForNewEntity(agoraEntity) {
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
        checkNamespacePermission(db, agoraEntity, username, AgoraPermissions(Redact)) {
          DBIO.sequence(configurations map { config => db.aePerms.deleteAllPermissions(config) }) andThen
            db.aePerms.deleteAllPermissions(agoraEntity)
        }
      }
    }
  }

  def copy(sourceArgs: AgoraEntity, targetArgs: AgoraEntity, redact: Boolean, entityTypes: Seq[AgoraEntityType.EntityType], username: String): Future[AgoraEntity] = {

    // we only allow this for methods.
    val dao = AgoraDao.createAgoraDao(Some(AgoraEntityType.Workflow))

    // get source. will throw exception if source does not exist
    val sourceEntity = dao.findSingle(sourceArgs)
    // munge the object we're inserting to be a copy of the source plus overrides from the new
    val entityToInsert = copyWithOverrides(sourceEntity, targetArgs).addDate()

    permissionsDataSource.inTransaction { db =>
      // do we have permissions to create a new snapshot?
      checkEntityPermission(db, sourceEntity, username, AgoraPermissions(Create)) {
        // insert target
        val targetEntity = dao.insert(entityToInsert)

        // get source permissions, copy to target
        (for {
          sourcePerms <- db.aePerms.listEntityPermissions(sourceEntity)
          stub <- db.aePerms.addEntity(targetEntity)
          targetPerms <- DBIO.sequence(sourcePerms map {
            db.aePerms.insertEntityPermission(targetEntity, _)
          })
        } yield targetEntity.removeIds()) cleanUp {
          case Some(t: Throwable) =>
            db.aePerms.deleteAllPermissions(targetEntity)
            throw new AgoraException("an unexpected error occurred during copy.", t)
          case None => DBIO.successful(targetEntity.removeIds())
        }
      }
    } flatMap { first =>
      val redactAction = if (redact)
        delete(sourceEntity, Seq(sourceEntity.entityType.get), username)
      else
        Future.successful(0)

      redactAction map {_ => first} recover {
        case t: Throwable => throw new AgoraException(
          "Copy completed, but failed to redact original.", t, StatusCodes.PartialContent
        )
      }
    }

  }

  private def copyWithOverrides(sourceEntity: AgoraEntity, newEntity: AgoraEntity): AgoraEntity = {
    // TODO: should we allow override of anything other than synopsis, doc, and payload?
    AgoraEntity(
      namespace = sourceEntity.namespace,
      name = sourceEntity.name,
      synopsis = if (newEntity.synopsis.isDefined) newEntity.synopsis else sourceEntity.synopsis,
      documentation = if (newEntity.documentation.isDefined) newEntity.documentation else sourceEntity.documentation,
      payload = if (newEntity.payload.isDefined) newEntity.payload else sourceEntity.payload,
      entityType=sourceEntity.entityType
    )
  }

  def listDefinitions(username: String): Future[Seq[MethodDefinition]] = {

    val methodsFuture = findWithIds(AgoraEntity(), None, Seq(AgoraEntityType.Workflow), username)
    val configsFuture = findWithIds(AgoraEntity(), None, Seq(AgoraEntityType.Configuration), username)

    configsFuture flatMap { configSnapshots =>
      methodsFuture map { methodSnapshots =>
        // group/count config snapshots by methodId
        val configCounts:Map[Option[ObjectId],Int] = configSnapshots
            .groupBy(_.methodId)
            .map { kv => kv._1 -> kv._2.size}

        // group method snapshots by namespace/name; count method snapshots and sum config counts
        val groupedMethods = methodSnapshots.groupBy( ae => (ae.namespace,ae.name))

        groupedMethods.values.map { aes:Seq[AgoraEntity] =>
          val numSnapshots:Int = aes.size
          val numConfigurations:Int = aes.map { ae => configCounts.getOrElse(ae.id, 0) }.sum

          // TODO: apply public status; how to define?
          // TODO: how to define owners, managers??
          // TODO: is using latest snapshot appropriate?
          // use the most recent (i.e. highest snapshot value) to populate the definition
          val latestSnapshot = aes.maxBy(_.snapshotId.getOrElse(Int.MinValue))
          MethodDefinition(latestSnapshot, numConfigurations, numSnapshots)
        }.toSeq
      }
    }
  }

  private def findWithIds(agoraSearch: AgoraEntity,
    agoraProjection: Option[AgoraEntityProjection],
    entityTypes: Seq[AgoraEntityType.EntityType],
    username: String): Future[Seq[AgoraEntity]] = {

    val entities = AgoraDao.createAgoraDao(entityTypes)
      .find(agoraSearch, agoraProjection)

    permissionsDataSource.inTransaction { db =>
      for {
        _ <- db.aePerms.addUserIfNotInDatabase(username)
        entity <- db.aePerms.filterEntityByRead(entities, username)
      } yield entity
    }
  }

  def find(agoraSearch: AgoraEntity,
           agoraProjection: Option[AgoraEntityProjection],
           entityTypes: Seq[AgoraEntityType.EntityType],
           username: String): Future[Seq[AgoraEntity]] =
    findWithIds(agoraSearch, agoraProjection, entityTypes, username).map { entities =>
      entities.map(_.addUrl().removeIds())
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
