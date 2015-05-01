package org.broadinstitute.dsde.agora.server.webservice.methods

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceHandlerProps, ServiceMessages}
import org.broadinstitute.dsde.agora.server.webservice.PerRequestCreator
import spray.routing.HttpService

@Api(value = "/methods", description = "Method Service", produces = "application/json", position = 1)
trait MethodsService extends HttpService with PerRequestCreator {
  this: ServiceHandlerProps => // Require a concrete ServiceHandlerProps creator to be mixed in
  
  val routes = queryRoute

  @ApiOperation(value = "Query a method from the method repository by namespace, name, and id",
    nickname = "methods",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[AgoraEntity],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "namespace", required = true, dataType = "string", paramType = "path", value = "Namespace"),
    new ApiImplicitParam(name = "name", required = true, dataType = "string", paramType = "path", value = "Name"),
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "Id")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Method Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  def queryRoute =
    path(ApiUtil.Methods.path) {
      parameters('namespace, 'name, 'id.as[Int]) { (namespace: String, name: String, id: Int)
      =>
        get {
          requestContext =>
            perRequest(requestContext, methodsQueryHandlerProps, ServiceMessages.Query(requestContext, namespace, name, id))
        }
      }
    }
}
