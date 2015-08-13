
package org.broadinstitute.dsde.agora.server.business

import cromwell.binding._
import cromwell.parser.WdlParser.SyntaxError
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.AgoraEntityPermissionsClient
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}

case class MethodImportResolver(username: String, agoraBusiness: AgoraBusiness) {
  def importResolver(importUri: String): WdlSource = {
    ImportResolverHelper.validateUri(importUri)
    val method = ImportResolverHelper.resolve(importUri, agoraBusiness, username)
    val canRead = AgoraEntityPermissionsClient.getEntityPermission(method, username).canRead
    if (canRead) method.payload.get
    else throw new SyntaxError(s"Can't resolve import: $importUri")
  }
}

object ImportResolverHelper {

  private val methodScheme = "methods://"

  def resolve(uri: String, agoraBusiness: AgoraBusiness, username: String): AgoraEntity = {
    val withoutScheme = removeScheme(uri)
    val parts = uriParts(withoutScheme)
    val namespace = parts(0)
    val name = parts(1)
    val snapshotId = parts(2).toInt
    val queryMethod = AgoraEntity(namespace = Option(namespace), name = Option(name), snapshotId = Option(snapshotId))
    val method = agoraBusiness.findSingle(queryMethod.namespace.get,
      queryMethod.name.get,
      queryMethod.snapshotId.get,
      Seq(AgoraEntityType.Task, AgoraEntityType.Workflow),
      username)
    method
  }

  def validateUri(importUri: String) = {
    if (!importUri.startsWith(methodScheme)) {
      throw new SyntaxError(s"Unsupported import uri: $importUri. Must start with $methodScheme.")
    }
    val withoutScheme = removeScheme(importUri)
    val parts = uriParts(withoutScheme)
    if (parts.length != 3) {
      throw new SyntaxError(s"Malformed methods uri. Must contain three parts, separated by '.': $importUri")
    }
    if (!isIdInt(parts(2))) {
      throw new SyntaxError(s"Malformed methods uri. SnapshotId must be integer: $importUri")
    }
  }

  private def removeScheme(uri: String): String = {
    uri.substring(methodScheme.length)
  }

  private def isIdInt(idStr: String): Boolean = {
    idStr.forall(_.isDigit)
  }

  private def uriParts(uriWithoutScheme: String): Array[String] = {
    uriWithoutScheme.split("\\.")
  }
}

