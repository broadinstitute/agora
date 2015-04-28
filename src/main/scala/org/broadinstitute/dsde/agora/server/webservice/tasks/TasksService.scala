package org.broadinstitute.dsde.agora.server.webservice.tasks

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.agora.server.webservice.{PerRequestCreator, TasksQueryResponse}
import org.broadinstitute.dsde.agora.server.webservice.util.{ApiUtil, ServiceMessages, ServiceHandlerProps}

import spray.routing.HttpService

@Api(value = "/tasks", description = "Task Service", produces = "application/json", position = 1)
trait TasksService extends HttpService with PerRequestCreator {
  this: ServiceHandlerProps => // Require a concrete ServiceHandlerProps creator to be mixed in
  
  val routes = queryRoute

  @ApiOperation(value = "Query a task from the method repository by method ID.",
    nickname = "tasks",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[TasksQueryResponse],
    notes = "API is rapidly changing.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "Task ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Task Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")
  ))
  def queryRoute =
    path(ApiUtil.Tasks.path / Segment) {
      id =>
        get {
            requestContext =>
              perRequest(requestContext, tasksQueryHandlerProps, ServiceMessages.Query(requestContext, id))
        }
    }
}
