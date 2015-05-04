package org.broadinstitute.dsde.agora.server.webservice.util

import akka.actor.Props
import org.broadinstitute.dsde.agora.server.webservice.methods.{MethodsAddHandler, MethodsQueryHandler}

/**
 * Generic trait to safely provide props for creating service handler actors.
 * Override to supply specific service handler implementations (ie Standard, Mock)
 */
trait ServiceHandlerProps {
  def methodsQueryHandlerProps = Props(new MethodsQueryHandler())
  
  def methodsAddHandlerProps = Props(new MethodsAddHandler())
}
