package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcProfile, TransactionIsolation}

import scala.concurrent.Future

class DataAccess(profile: JdbcProfile) {
  val nsPerms = new NamespacePermissionsClient(profile)
  val aePerms = new AgoraEntityPermissionsClient(profile)
  val admPerms = new AdminPermissionsClient(profile)
}

class PermissionsDataSource(databaseConfig: DatabaseConfig[JdbcProfile]) {
  val profile = databaseConfig.profile
  val db = databaseConfig.db

  import profile.api._

  val dataAccess = new DataAccess(profile)

  def inTransaction[T](f: (DataAccess) => ReadWriteAction[T], isolationLevel: TransactionIsolation = TransactionIsolation.RepeatableRead): Future[T] = {
    db.run(f(dataAccess).transactionally.withTransactionIsolation(isolationLevel))
  }

  def close(): Unit = {
    db.close()
  }
}
