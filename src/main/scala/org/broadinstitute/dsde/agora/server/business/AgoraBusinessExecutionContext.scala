package org.broadinstitute.dsde.agora.server.business

import scala.concurrent.ExecutionContext

/**
 * Wraps the underlying execution context that should be used for slow operations.
 *
 * By using a separate class type, we can still use implicits.
 *
 * @param executionContext The execution context to use for slow IO / business operations.
 */
class AgoraBusinessExecutionContext(val executionContext: ExecutionContext)
