package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.AgoraConfig
import org.flywaydb.core.Flyway
import slick.jdbc.DriverDataSource
import slick.util.BeanConfigurator
import slick.util.ConfigExtensionMethods._

object TestPermissionsClient {

  private lazy val flyway: Flyway = {
    // Create a DataSource for configuring Flyway.
    // Based off:
    //   https://github.com/slick/slick/blob/v3.3.3/slick/src/main/scala/slick/jdbc/JdbcDataSource.scala#L100-L104
    val driverDataSource = new DriverDataSource
    BeanConfigurator.configure(
      driverDataSource,
      AgoraConfig.sqlConfig.getConfig("db").toProperties,
      Set("url", "user", "password", "properties", "driver", "driverClassName"),
    )

    Flyway.configure().dataSource(driverDataSource).load()
  }

  def ensureRunning(): Unit = flyway.migrate()

  def clean(): Unit = flyway.clean()
}
