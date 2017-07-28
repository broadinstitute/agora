
package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.exceptions
import org.broadinstitute.dsde.agora.server.exceptions.{AgoraException, ValidationException}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import spray.http.StatusCodes

import scala.annotation.meta.field
import scalaz.Scalaz._
import scalaz._

object AgoraEntityType extends Enumeration {
  def byPath(path: String): Seq[EntityType] = path match {
    case AgoraConfig.methodsRoute => Seq(Task, Workflow)
    case AgoraConfig.configurationsRoute => Seq(Configuration)
  }

  type EntityType = Value
  val Task = Value("Task")
  val Workflow = Value("Workflow")
  val Configuration = Value("Configuration")
  val MethodTypes = Seq(Task, Workflow)
}

object AgoraEntity {

  // ValidationNel is a Non-empty List (Nel) data structure. The left type
  // is the failure type. The right type is the success type.
  //
  // When completing a ValidationNel, both types must be provided.
  //
  // Examples to complete a ValidationNel[String, Int]:
  // Obj[SuccessType].successNel[FailureType] = 1.successNel[String]
  // Obj[FailureType].failureNel[SuccessType] = "fail".failureNel[Int]

  // Note that newly-created entities are subject to stricter rules than these
  // See validateNamesForNewEntity(), GAWB-1614
  def validate(entity: AgoraEntity, allowEmptyIdentifiers: Boolean = true): ValidationNel[String, Boolean] = {

    def validateNamespace(namespace: String): ValidationNel[String, String] = {
      if (namespace.trim.nonEmpty) namespace.successNel[String]
      else "Namespace cannot be empty".failureNel[String]
    }

    def validateName(name: String): ValidationNel[String, String] = {
      if (name.trim.nonEmpty) name.successNel[String]
      else "Name cannot be empty".failureNel[String]
    }

    def validateSnapshotId(_id: Int): ValidationNel[String, Int] = {
      if (_id > 0) _id.successNel[String]
      else "SnapshotId must be greater than 0".failureNel[Int]
    }

    def validateSynopsis(synopsis: String): ValidationNel[String, String] = {
      if (synopsis.length <= 80) synopsis.successNel[String]
      else "Synopsis must be less than 80 chars".failureNel[String]
    }

    def validateDocumentation(doc: String): ValidationNel[String, String] = {
      if (doc.getBytes.size <= 10000) doc.successNel[String]
      else "Documentation must be less than 10kb".failureNel[String]
    }

    val namespace = entity.namespace match {
      case Some(n) => validateNamespace(n)
      case None =>
        if (allowEmptyIdentifiers)
          None.successNel[String]
        else
          "Namespace must be supplied".failureNel[String]
    }

    val name = entity.name match {
      case Some(n) => validateName(n)
      case None =>
        if (allowEmptyIdentifiers)
          None.successNel[String]
        else
          "Namespace must be supplied".failureNel[String]
    }

    val _id = entity.snapshotId match {
      case Some(snapShotId) => validateSnapshotId(snapShotId)
      case None =>
        if (allowEmptyIdentifiers)
          None.successNel[String]
        else
          "SnapshotId must be supplied".failureNel[Int]
    }

    val synopsis = entity.synopsis match {
      case Some(s) => validateSynopsis(s)
      case None => None.successNel[String]
    }

    val doc = entity.documentation match {
      case Some(docs) => validateDocumentation(docs)
      case None => None.successNel[String]
    }

    def doNothing() = true

    // The |@| operator is a combinator that combines the validations into a single object
    // This allows all of the errors to be returned at once!
    (namespace |@| name |@| _id |@| synopsis |@| doc) {(namespace, name, _id, synopsis, doc) => doNothing }
  }

}

case class AgoraEntity(namespace: Option[String] = None,
                       name: Option[String] = None,
                       snapshotId: Option[Int] = None,
                       synopsis: Option[String] = None,
                       documentation: Option[String] = None,
                       owner: Option[String] = None,
                       createDate: Option[DateTime] = None,
                       payload: Option[String] = None,
                       url: Option[String] = None,
                       entityType: Option[AgoraEntityType.EntityType] = None,
                       id: Option[ObjectId] = None,
                       methodId: Option[ObjectId] = None,
                       method: Option[AgoraEntity] = None,
                       managers: Seq[String] = Seq(),
                       public: Option[Boolean] = None) {

  AgoraEntity.validate(this) match {
    case Success(_) => this
    case Failure(errors) => throw new ValidationException(s"Entity is not valid: $errors")
  }

  def agoraUrl: String = {
    AgoraConfig.urlFromType(entityType) + namespace.get + "/" + name.get + "/" + snapshotId.get
  }

  def addUrl(): AgoraEntity = {
    copy(url = Option(agoraUrl))
  }

  def addDate(): AgoraEntity = {
    copy(createDate = Option(new DateTime()))
  }
  
  def addMethodId(methodId: String): AgoraEntity = {
    copy(methodId = Option(new ObjectId(methodId)))
  }

  def addMethod(method: Option[AgoraEntity]): AgoraEntity = {
    copy(method = method)
  }
  
  def removeIds(): AgoraEntity = {
    copy(id = None, methodId = None)
  }

  def addEntityType(entityType: Option[AgoraEntityType.EntityType]): AgoraEntity = {
    copy(entityType = entityType)
  }

  def addManagers(managers: Seq[String]): AgoraEntity = {
    copy(managers = managers)
  }

  def toShortString: String = s"AgoraEntity($namespace,$name,$snapshotId)"
}



