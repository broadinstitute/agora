package org.broadinstitute.dsde.agora.server.busines

import com.google.api.services.storage.model.{ObjectAccessControl, BucketAccessControl}
import org.broadinstitute.dsde.agora.server.business.{EntityAuthorizationException, NamespaceAuthorizationException}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AgoraPermissions.Manage
import org.broadinstitute.dsde.agora.server.dataaccess.acls.{AgoraPermissions, AuthorizationProvider}
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}
import org.broadinstitute.dsde.agora.server.dataaccess.acls.gcs.GcsClient._
import spray.routing.RequestContext

//TODO: Lots to refactor in this class. too much duplication
class AclBusiness(authorizationProvider: AuthorizationProvider) {

  def listNamespaceAcls(context: RequestContext, entity: AgoraEntity, user: String) = {
    if (authorizationProvider.namespaceAuthorization(entity, user).canManage) {
      val bucket = entityToBucket(entity)
      val bucketAcls = listBucketAcl(bucket).setFields("items(entity,role)").execute()

      bucketAcls.toPrettyString

    } else {
      throw new NamespaceAuthorizationException(AgoraPermissions(Manage), entity)
    }
  }

  def insertNamespaceAcl(_context: RequestContext, entity: AgoraEntity, user: String, acl: BucketAccessControl) = {
    if (authorizationProvider.namespaceAuthorization(entity, user).canManage) {
      val bucket = entityToBucket(entity)
      val bucketAcls = getBucket(bucket).setProjection("full").execute().getAcl

      // Filter any existing acls that reference user to be added.
      val updatedAcls = filterBucketAclsByEntity(bucketAcls, acl.getEntity)

      updatedAcls.add(acl)
      patchBucketAcls(bucket, updatedAcls).setFields("acl(entity,role)").execute().toPrettyString

    } else {
      throw new NamespaceAuthorizationException(AgoraPermissions(Manage), entity)
    }
  }

  def deleteNamespaceAcl(_context: RequestContext, entity: AgoraEntity, user: String, acl: BucketAccessControl) = {
    if (authorizationProvider.namespaceAuthorization(entity, user).canManage) {
      val bucket = entityToBucket(entity)
      val bucketAcls = getBucket(bucket).setProjection("full").execute().getAcl

      // Filter any existing acls that reference the user to be deleted.
      val updatedAcls = filterBucketAclsByEntity(bucketAcls, acl.getEntity)

      patchBucketAcls(bucket, updatedAcls).setFields("acl(entity,role)").execute().toPrettyString

    } else {
      throw new NamespaceAuthorizationException(AgoraPermissions(Manage), entity)
    }
  }

  def filterBucketAclsByEntity(acls: java.util.List[BucketAccessControl], entityToFilter: String): java.util.ArrayList[BucketAccessControl] = {
    val numberOfAcls = acls.size
    val updatedAcls = new java.util.ArrayList[BucketAccessControl]()
    for (i <- 0 until numberOfAcls) {
      if (acls.get(i).getEntity != entityToFilter) {
        updatedAcls.add(acls.get(i))
      }
    }
    return updatedAcls
  }

  def listEntityAcls(context: RequestContext, entity: AgoraEntity, user: String) = {
    if (authorizationProvider.entityAuthorization(entity, user).canManage) {
      val _object = entityToObject(entity)
      val objectAcls = listObjectAcl(_object).setFields("items(entity,role)").execute()

      objectAcls.toPrettyString

    } else {
      throw new EntityAuthorizationException(AgoraPermissions(Manage), entity)
    }
  }

  def insertEntityAcl(_context: RequestContext, entity: AgoraEntity, user: String, acl: ObjectAccessControl) = {
    if (authorizationProvider.namespaceAuthorization(entity, user).canManage) {
      val _object = entityToObject(entity)
      val objectAcls = getObject(_object).setProjection("full").execute().getAcl

      // Filter any existing acls that reference user to be added.
      val updatedAcls = filterObjectAclsByEntity(objectAcls, acl.getEntity)

      updatedAcls.add(acl)
      patchObjectAcls(_object, updatedAcls).setFields("acl(entity,role)").execute().toPrettyString

    } else {
      throw new EntityAuthorizationException(AgoraPermissions(Manage), entity)
    }
  }

  def deleteEntityAcl(_context: RequestContext, entity: AgoraEntity, user: String, acl: ObjectAccessControl) = {
    if (authorizationProvider.namespaceAuthorization(entity, user).canManage) {
      val _object = entityToObject(entity)
      val objectAcls = getObject(_object).setProjection("full").execute().getAcl

      // Filter any existing acls that reference the user to be deleted.
      val updatedAcls = filterObjectAclsByEntity(objectAcls, acl.getEntity)

      patchObjectAcls(_object, updatedAcls).setFields("acl(entity,role)").execute().toPrettyString

    } else {
      throw new EntityAuthorizationException(AgoraPermissions(Manage), entity)
    }
  }

  def filterObjectAclsByEntity(acls: java.util.List[ObjectAccessControl], entityToFilter: String): java.util.ArrayList[ObjectAccessControl] = {
    val numberOfAcls = acls.size
    val updatedAcls = new java.util.ArrayList[ObjectAccessControl]()
    for (i <- 0 until numberOfAcls) {
      if (acls.get(i).getEntity != entityToFilter) {
        updatedAcls.add(acls.get(i))
      }
    }
    return updatedAcls
  }

}
