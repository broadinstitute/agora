resolvers += "Flyway" at "https://flywaydb.org/repo"

addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "6.5.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")

addSbtPlugin("io.spray" % "sbt-revolver" % "0.9.1")
