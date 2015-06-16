package org.broadinstitute.dsde.agora.server.model

case class ProjectTeam(projectNumber: Option[String], team: Option[String])

case class GoogleAccessControls(kind: Option[String],
                                id: Option[String],
                                selfLink: Option[String],
                                bucket: Option[String],
                                `object`: Option[String],
                                generation: Option[String],
                                entity: Option[String],
                                role: Option[String], // READER, WRITER, or OWNER. TODO- use enum?
                                email: Option[String],
                                entityId: Option[String],
                                domain: Option[String],
                                projectTeam: Option[ProjectTeam],
                                etag: Option[String])

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

case class BucketResource(name: Option[String] = None,
                          timeCreated: Option[String] = None, // datetime -> rfc3339 format
                          metageneration: Option[Long] = None,
                          acl: List[String] = Nil, // [ bucketAccessControls Resource ] ?
                          defaultObjectAcl: List[String] = Nil, // [ defaultObjectAccessControls Resource ] ?
                          owner: Option[Owner] = None,
                          location: Option[String] = None,
                          website: Option[Website] = None,
                          logging: Option[Logging] = None,
                          versioning: Option[Versioning] = None,
                          cors: List[Cors] = Nil,
                          lifecycle: Option[Lifecycle] = None,
                          storageClass: Option[String] = None,
                          etag: Option[String] = None)
