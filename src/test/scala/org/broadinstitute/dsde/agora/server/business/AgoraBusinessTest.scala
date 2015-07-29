package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.acls.MockAuthorizationProvider
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}

@DoNotDiscover
class AgoraBusinessTest extends FlatSpec with Matchers {

  val agoraBusiness = new AgoraBusiness(MockAuthorizationProvider)
  val methodImportResolver = new MethodImportResolver(agoraCIOwner.get, agoraBusiness, MockAuthorizationProvider)

  "Agora" should "not find a method payload when resolving a WDL import statement if the method has not been added" in {
    val importString = "methods://broad.nonexistent.5400"
    intercept[Exception] {
      methodImportResolver.importResolver(importString)
    }
  }

  "Agora" should "throw an exception when trying to resolve a WDL import that is improperly formatted" in {
    val importString = "methods:broad.nonexistent.5400"
    intercept[Exception] {
      methodImportResolver.importResolver(importString)
    }
  }
}
