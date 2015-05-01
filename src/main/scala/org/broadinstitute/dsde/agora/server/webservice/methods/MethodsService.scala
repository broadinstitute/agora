package org.broadinstitute.dsde.agora.server.webservice.methods

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps, ServiceMessages}
import org.broadinstitute.dsde.agora.server.webservice.{MethodsQueryResponse, PerRequestCreator}
import spray.routing.HttpService

@Api(value = "/methods", description = "Method Service", produces = "application/json", position = 1)
trait MethodsService extends HttpService with PerRequestCreator {
  this: ServiceHandlerProps => // Require a concrete ServiceHandlerProps creator to be mixed in
  
  val routes = queryRoute

  @ApiOperation(value = "Query a method from the method repository by method ID.",
    nickname = "methods",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[MethodsQueryResponse],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "Method Id")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Method Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  def queryRoute =
    path(ApiUtil.Methods.path / Segment) {
      id =>
        get {
            requestContext =>
              perRequest(requestContext, methodsQueryHandlerProps, ServiceMessages.Query(requestContext, id))
        }
    }
}
