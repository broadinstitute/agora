package org.broadinstitute.dsde.agora.server.webservice.methods

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.agora.server.dataaccess.acls.AuthorizationProvider
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.AgoraService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

/**
 * The MethodsService is a light wrapper around AgoraService.
 *
 * This file defines a methods path (config) and Swagger annotations.
 */

@Api(value = "/methods", description = "Method Service", produces = "application/json", position = 1)
abstract class MethodsService(authorizationProvider: AuthorizationProvider) extends AgoraService(authorizationProvider) {
  override def path = ApiUtil.Methods.path

  @ApiOperation(value = "Get a method in the methods repository matching namespace, name, and snapshot id",
    nickname = "methods",
    httpMethod = "GET",
    produces = "application/json,text/plain",
    response = classOf[AgoraEntity],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "namespace", required = true, dataType = "string", paramType = "path", value = "Namespace"),
    new ApiImplicitParam(name = "name", required = true, dataType = "string", paramType = "path", value = "Name"),
    new ApiImplicitParam(name = "snapshotId", required = true, dataType = "string", paramType = "path", value = "SnapshotId"),
    new ApiImplicitParam(name = "onlyPayload", required = false, allowMultiple = false, dataType = "string", paramType = "query", value = "OnlyPayload",
      allowableValues = "true,false")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Method Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  override def querySingleRoute = super.querySingleRoute

  @ApiOperation(value = "Query for method in the methods repository",
    nickname = "methods",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[AgoraEntity],
    responseContainer = "Seq",
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "namespace", required = false, dataType = "string", paramType = "query", value = "Namespace"),
    new ApiImplicitParam(name = "name", required = false, dataType = "string", paramType = "query", value = "Name"),
    new ApiImplicitParam(name = "snapshotId", required = false, dataType = "string", paramType = "query", value = "SnapshotId"),
    new ApiImplicitParam(name = "synopsis", required = false, dataType = "string", paramType = "query", value = "Synopsis"),
    new ApiImplicitParam(name = "documentation", required = false, dataType = "string", paramType = "query", value = "Documentation"),
    new ApiImplicitParam(name = "owner", required = false, dataType = "string", paramType = "query", value = "Owner"),
    new ApiImplicitParam(name = "payload", required = false, dataType = "string", paramType = "query", value = "Payload"),
    new ApiImplicitParam(name = "entityType", required = false, dataType = "string", paramType = "query", value = "EntityType", allowableValues = "Task,Workflow"),
    new ApiImplicitParam(name = "excludedField", required = false, allowMultiple = true, dataType = "string", paramType = "query", value = "Excluded Field"),
    new ApiImplicitParam(name = "includedField", required = false, allowMultiple = true, dataType = "string", paramType = "query", value = "Included Field")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  override def queryRoute = super.queryRoute

  @ApiOperation(value = "Add a method to the methods repository",
    nickname = "add",
    httpMethod = "POST",
    produces = "application/json",
    response = classOf[AgoraEntity],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.agora.server.model.AgoraEntity", paramType = "body", value = "Agora Entity")))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "Method Successfully Added"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  override def postRoute = super.postRoute
}
