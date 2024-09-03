package org.broadinstitute.dsde.agora

import org.broadinstitute.dsde.workbench.model.ErrorReportSource

package object server {
  implicit val errorReportSource: ErrorReportSource = ErrorReportSource("agora")
}
