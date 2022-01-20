package org.broadinstitute.dsde.rawls

/**
  * This rawls.model package has been copy-pasted from the rawls source code.
  *
  * It is a much-slimmed-down version of what exists in rawls, only copy-pasting the parts Agora absolutely needs.
  *
  * We do this in anticipation of upgrading Agora to Scala 2.12, ahead of a Rawls upgrade to Scala 2.12.
  * Once Agora is on 2.12, it cannot import the 2.11 rawls-model. Therefore, we are removing the dependency
  * right now.
  *
  * TODO: once Rawls is upgraded to 2.12, remove this package from the Agora source tree, and re-import rawls-model.
  *
  */
package object model {}
