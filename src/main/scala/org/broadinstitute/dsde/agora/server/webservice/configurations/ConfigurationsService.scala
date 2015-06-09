
package org.broadinstitute.dsde.agora.server.webservice.configurations

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.AgoraService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil

@Api(value = "/configurations", description = "Configuration Service", produces = "application/json", position = 1)
trait ConfigurationsService extends AgoraService {
  override def path = ApiUtil.Configurations.path

  @ApiOperation(value = "Get a task configuration in the task configuration repository matching namespace, name, and snapshot id",
    nickname = "configurations",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[AgoraEntity],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "namespace", required = true, dataType = "string", paramType = "path", value = "Namespace"),
    new ApiImplicitParam(name = "name", required = true, dataType = "string", paramType = "path", value = "Name"),
    new ApiImplicitParam(name = "snapshotId", required = true, dataType = "string", paramType = "path", value = "SnapshotId")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Task Configuration Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  override def queryByNamespaceNameSnapshotIdRoute = super.queryByNamespaceNameSnapshotIdRoute

  @ApiOperation(value = "Query for task configuration in the task configuration repository",
    nickname = "configurations",
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
    new ApiImplicitParam(name = "excludedField", required = false, allowMultiple = true, dataType = "string", paramType = "query", value = "Excluded Field"),
    new ApiImplicitParam(name = "includedField", required = false, allowMultiple = true, dataType = "string", paramType = "query", value = "Included Field")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  override def queryRoute = super.queryRoute

  @ApiOperation(value = "Add a task configuration to the task configuration repository",
    nickname = "add",
    httpMethod = "POST",
    produces = "application/json",
    response = classOf[AgoraEntity],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.agora.server.model.AgoraEntity", paramType = "body", value = "Agora Entity")))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "Task Configuration Successfully Added"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  override def postRoute = super.postRoute
}
