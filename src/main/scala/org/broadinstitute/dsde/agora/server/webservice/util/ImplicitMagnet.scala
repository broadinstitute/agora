package org.broadinstitute.dsde.agora.server.webservice.util

import scala.language.implicitConversions

/*
Based on http://spray.io/blog/2012-12-13-the-magnet-pattern/
First usage was for pulling in the implicit ExecutionContext for use with Future instances.
*/
class ImplicitMagnet[T](val value: T)

object ImplicitMagnet {
  // The "u: Unit" allows "foo()". http://spray.io/blog/2012-12-13-the-magnet-pattern/#param-list-required
  // You may need to use "foo(())" to avoid feature deprecation in 2.11+. http://stackoverflow.com/a/25354165/3320205
  implicit def toImplicitMagnet[A](u: Unit)(implicit a: A): ImplicitMagnet[A] = new ImplicitMagnet[A](a)
}
