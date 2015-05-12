package org.broadinstitute.dsde.agora.server.webservice.methods

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.PerRequestCreator
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps, ServiceMessages}
import org.joda.time.DateTime
import spray.routing.HttpService

@Api(value = "/methods", description = "Method Service", produces = "application/json", position = 1)
trait MethodsService extends HttpService with PerRequestCreator {
  this: ServiceHandlerProps =>
  // Require a concrete ServiceHandlerProps creator to be mixed in

  import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
  import spray.httpx.SprayJsonSupport._

  val routes = queryByNamespaceNameSnapshotIdRoute ~ queryRoute ~ postRoute

  @ApiOperation(value = "Get a method in the method repository matching namespace, name, and snapshot id",
    nickname = "methods",
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
    new ApiResponse(code = 404, message = "Method Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  def queryByNamespaceNameSnapshotIdRoute =
    path(ApiUtil.Methods.path / Segment / Segment / Segment) { (namespace, name, snapshotId) =>
      get {
        requestContext =>
          perRequest(requestContext, methodsQueryHandlerProps, ServiceMessages.QueryByNamespaceNameSnapshotId(requestContext, namespace, name, snapshotId.toInt))
      }
    }

  @ApiOperation(value = "Query for methods in the method repository",
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
    new ApiImplicitParam(name = "payload", required = false, dataType = "string", paramType = "query", value = "Payload")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  def queryRoute =
    path(ApiUtil.Methods.path) {
      get {
        parameters("namespace".?, "name".?, "snapshotId".as[Int].?, "synopsis".?, "documentation".?, "owner".?, "createDate".as[DateTime].?, "payload".?).as(AgoraEntity) { agoraEntity =>
          requestContext =>
            perRequest(requestContext, methodsQueryHandlerProps, ServiceMessages.Query(requestContext, agoraEntity))
        }
      }
    }

  @ApiOperation(value = "Add a method to the method repository",
    nickname = "add",
    httpMethod = "POST",
    produces = "application/json",
    response = classOf[AgoraEntity],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.agora.server.model.AgoraEntity", paramType = "body", value = "Agora Entity")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  def postRoute =
    path(ApiUtil.Methods.path) {
      post {
        entity(as[AgoraEntity]) { agoraEntity =>
          requestContext =>
            perRequest(requestContext, methodsAddHandlerProps, ServiceMessages.Add(requestContext, agoraEntity))
        }
      }
    }


}
