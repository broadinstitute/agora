package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import slick.backend.DatabaseConfig
import slick.driver.{JdbcDriver, MySQLDriver}
import slick.jdbc.TransactionIsolation

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class DataAccess {
  val namespacePermissionsClient = new NamespacePermissionsClient()
  val agoraEntityPermissionsClient = new AgoraEntityPermissionsClient()
}

class PermissionsDataSource(databaseConfig: DatabaseConfig[MySQLDriver]) {
  import databaseConfig.driver.api._

  val db = databaseConfig.db
  val dataAccess = new DataAccess() // <- probably db goes in here

  def inTransaction[T](f: (DataAccess) => ReadWriteAction[T], isolationLevel: TransactionIsolation = TransactionIsolation.RepeatableRead): Future[T] = {
    //database.run(f(dataAccess).transactionally) <-- https://github.com/slick/slick/issues/1274
    Future(Await.result(db.run(f(dataAccess).transactionally.withTransactionIsolation(isolationLevel)), Duration.Inf))
  }
}
