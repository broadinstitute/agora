
package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.broadinstitute.dsde.agora.server.exceptions.AgoraException
import org.bson.types.ObjectId
import java.time.OffsetDateTime
import cats.data.Validated._
import cats.data.ValidatedNel
import cats.syntax.apply._
import cats.syntax.validated._
import org.broadinstitute.dsde.rawls.model.MethodConfiguration
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport.MethodConfigurationFormat
import spray.json._
import org.broadinstitute.dsde.rawls.model.JsonSupport
import spray.json.JsonParser.ParsingException



object AgoraEntityType extends Enumeration {
  def byPath(path: String): Seq[EntityType] = path match {
    case AgoraConfig.methodsRoute => Seq(Task, Workflow)
    case AgoraConfig.configurationsRoute => Seq(Configuration)
  }

  type EntityType = Value
  val Task: EntityType = Value("Task")
  val Workflow: EntityType = Value("Workflow")
  val Configuration: EntityType = Value("Configuration")
  val MethodTypes: Seq[EntityType] = Seq(Task, Workflow)
}

object AgoraEntity extends JsonSupport {

  // ValidatedNel is a Non-empty List (Nel) data structure. The left type
  // is the failure type. The right type is the success type.
  //
  // When completing a ValidatedNel, both types must be provided.
  //
  // Examples to complete a ValidatedNel[String, Int]:
  // Obj[SuccessType].validNel[FailureType] = 1.validNel[String]
  // Obj[FailureType].invalidNel[SuccessType] = "fail".invalidNel[Int]

  // Note that newly-created entities are subject to stricter rules than these
  // See validateNamesForNewEntity(), GAWB-1614
  def validate(entity: AgoraEntity, allowEmptyIdentifiers: Boolean = true): ValidatedNel[String, Boolean] = {

    def validateNamespace(namespace: String): ValidatedNel[String, String] = {
      if (namespace.trim.nonEmpty) namespace.validNel[String]
      else "Namespace cannot be empty".invalidNel[String]
    }

    def validateName(name: String): ValidatedNel[String, String] = {
      if (name.trim.nonEmpty) name.validNel[String]
      else "Name cannot be empty".invalidNel[String]
    }

    def validateSnapshotId(_id: Int): ValidatedNel[String, Int] = {
      if (_id > 0) _id.validNel[String]
      else "SnapshotId must be greater than 0".invalidNel[Int]
    }

    def validateSynopsis(synopsis: String): ValidatedNel[String, String] = {
      if (synopsis.length <= 80) synopsis.validNel[String]
      else "Synopsis must be less than 80 chars".invalidNel[String]
    }

    def validateDocumentation(doc: String): ValidatedNel[String, String] = {
      if (doc.getBytes.length <= 10000) doc.validNel[String]
      else "Documentation must be less than 10kb".invalidNel[String]
    }

    val namespace = entity.namespace match {
      case Some(n) => validateNamespace(n)
      case None =>
        if (allowEmptyIdentifiers)
          None.validNel[String]
        else
          "Namespace must be supplied".invalidNel[String]
    }

    val name = entity.name match {
      case Some(n) => validateName(n)
      case None =>
        if (allowEmptyIdentifiers)
          None.validNel[String]
        else
          "Namespace must be supplied".invalidNel[String]
    }

    val _id = entity.snapshotId match {
      case Some(snapShotId) => validateSnapshotId(snapShotId)
      case None =>
        if (allowEmptyIdentifiers)
          None.validNel[String]
        else
          "SnapshotId must be supplied".invalidNel[Int]
    }

    val synopsis = entity.synopsis match {
      case Some(s) => validateSynopsis(s)
      case None => None.validNel[String]
    }

    val doc = entity.documentation match {
      case Some(docs) => validateDocumentation(docs)
      case None => None.validNel[String]
    }

    def doNothing() = true

    // The mapN operator is a combinator that combines the validations into a single object
    // This allows all of the errors to be returned at once!
    (namespace, name, _id, synopsis, doc) mapN { (_, _, _, _, _) => doNothing() }
  }

}

case class AgoraEntity(namespace: Option[String] = None,
                       name: Option[String] = None,
                       snapshotId: Option[Int] = None,
                       snapshotComment: Option[String] = None,
                       synopsis: Option[String] = None,
                       documentation: Option[String] = None,
                       owner: Option[String] = None,
                       createDate: Option[OffsetDateTime] = None,
                       payload: Option[String] = None,
                       payloadObject: Option[MethodConfiguration] = None,
                       url: Option[String] = None,
                       entityType: Option[AgoraEntityType.EntityType] = None,
                       id: Option[ObjectId] = None,
                       methodId: Option[ObjectId] = None,
                       method: Option[AgoraEntity] = None,
                       managers: Seq[String] = Seq(),
                       public: Option[Boolean] = None) {

  lazy val entityAlias: String =
    namespace.get + "." + name.get + "." + snapshotId.get

  lazy val namespaceAlias: String =
    namespace.get

  def agoraUrl: String = {
    AgoraConfig.urlFromType(entityType) + namespace.get + "/" + name.get + "/" + snapshotId.get
  }

  def addUrl(): AgoraEntity = {
    copy(url = Option(agoraUrl))
  }

  def addDate(): AgoraEntity = {
    copy(createDate = Option(OffsetDateTime.now()))
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

  def addIsPublic(isPublic: Boolean): AgoraEntity = {
    copy(public = Some(isPublic))
  }

  def toShortString: String = s"AgoraEntity($namespace,$name,$snapshotId)"

  def canDeserializePayload: Boolean = {
    entityType.contains(AgoraEntityType.Configuration)
  }

  def withDeserializedPayload: AgoraEntity = {
    if (!canDeserializePayload) throw AgoraException(s"Entity type $entityType does not support payload deserialization")

    payload match {
      case Some(pl: String) =>
        try {
          val parsed = pl.parseJson
          val deserialized = parsed.convertTo[MethodConfiguration]

          this.copy(
            payloadObject = Some(deserialized),
            payload = None
          )

        } catch {
          case parseFail: ParsingException =>
            throw AgoraException(s"Payload for $toShortString could not be deserialized: ${parseFail.summary}")
          case deserializeFail: DeserializationException =>
            throw AgoraException(s"Payload for $toShortString is valid JSON but mapping to object model failed: ${deserializeFail.msg}")
        }
      case _ => this
    }
  }
}

object MethodDefinition {
  def apply(ae:AgoraEntity, managers: Seq[String], isPublic: Boolean, numConfigurations: Int, numSnapshots: Int): MethodDefinition =
    new MethodDefinition(ae.namespace,
      ae.name,
      ae.synopsis,
      ae.entityType,
      managers,
      Some(isPublic),
      numConfigurations,
      numSnapshots)
}

case class MethodDefinition(namespace: Option[String] = None,
                       name: Option[String] = None,
                       synopsis: Option[String] = None,
                       entityType: Option[AgoraEntityType.EntityType] = None,
                       managers: Seq[String] = Seq(),
                       public: Option[Boolean] = None,
                       numConfigurations: Int,
                       numSnapshots: Int)

