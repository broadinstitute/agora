package org.broadinstitute.dsde.agora.server.model

case class ProjectTeam(projectNumber: Option[String], team: Option[String])

object GoogleAccessControls {
  def bucketKind = "storage#bucketAccessControl"
  def objectKind = "storage#objectAccessControl"
}

case class GoogleAccessControls(kind: Option[String],
                                id: Option[String] = None,
                                selfLink: Option[String] = None,
                                bucket: Option[String] = None,
                                `object`: Option[String] = None,
                                generation: Option[String] = None,
                                entity: Option[String],
                                role: Option[String], // READER, WRITER, or OWNER. TODO- use enum?
                                email: Option[String] = None,
                                entityId: Option[String] = None,
                                domain: Option[String] = None,
                                projectTeam: Option[ProjectTeam] = None,
                                etag: Option[String] = None)

case class Owner(entity: Option[String], entityId: Option[String])

case class Website(mainPageSuffix: Option[String], notFoundPage: Option[String])

case class Logging(logBucket: Option[String], logObjectPrefix: Option[String])

case class Versioning(enabled: Option[Boolean])

case class Cors(origin: List[String],
                method: List[String],
                responseHeader: List[String],
                maxAgeSeconds: Option[String])

case class Action(`type`: Option[String])

case class Condition(age: Option[String],
                     createdBefore: Option[String],
                     isLive: Option[Boolean],
                     numNewerVersions: Option[String])

case class Rule(action: Option[Action], condition: Option[Condition])

case class Lifecycle(rule: List[Rule])

case class BucketName(name: String)

case class Metadata(key: String, value: String)

case class BucketResource(name: Option[String] = None,
                          timeCreated: Option[String] = None, // datetime -> rfc3339 format
                          metageneration: Option[Long] = None,
                          acl: Option[Seq[GoogleAccessControls]] = None,
                          defaultObjectAcl: Option[Seq[GoogleAccessControls]] = None,
                          owner: Option[Owner] = None,
                          location: Option[String] = None,
                          website: Option[Website] = None,
                          logging: Option[Logging] = None,
                          versioning: Option[Versioning] = None,
                          cors: Option[Seq[Cors]] = None,
                          lifecycle: Option[Lifecycle] = None,
                          storageClass: Option[String] = None,
                          etag: Option[String] = None)


// Two fields are commented, so object can be serialized with jsonFormat22.
case class ObjectResource(kind: Option[String] = None,
                          id: Option[String] = None,
                          selfLink: Option[String] = None,
                          name: Option[String] = None,
                          bucket: Option[String] = None,
                          generation: Option[Long] = None,
//                          metageneration: Option[Long] = None,
                          contentType: Option[String] = None,
                          updated: Option[String] = None,
                          timeDeleted: Option[String] = None,
                          storageClass: Option[String] = None,
                          size: Option[Long] = None,
                          md5Hash: Option[String] = None,
                          mediaLink: Option[String] = None,
                          contentEncoding: Option[String] = None,
//                          contentDisposition: Option[String] = None,
                          contentLanguage: Option[String] = None,
                          cacheControl: Option[String] = None,
                          metadata: Option[Metadata] = None,
                          acl: Option[Seq[GoogleAccessControls]] = None,
                          owner: Option[Owner] = None,
                          crc32c: Option[String] = None,
                          componentCount: Option[Int] = None,
                          etag: Option[String] = None)
