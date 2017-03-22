package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcProfile, TransactionIsolation}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class DataAccess(profile: JdbcProfile) {
  val nsPerms = new NamespacePermissionsClient(profile)
  val aePerms = new AgoraEntityPermissionsClient(profile)
}

class PermissionsDataSource(databaseConfig: DatabaseConfig[JdbcProfile]) {
  val profile = databaseConfig.profile
  val db = databaseConfig.db

  import profile.api._

  val dataAccess = new DataAccess(profile)

  def inTransaction[T](f: (DataAccess) => ReadWriteAction[T], isolationLevel: TransactionIsolation = TransactionIsolation.RepeatableRead): Future[T] = {
    //FIXME: still needs custom executor. see rawls:
    // https://github.com/broadinstitute/rawls/blob/develop/core/src/main/scala/org/broadinstitute/dsde/rawls/dataaccess/DataSource.scala#L52
    Future(Await.result(db.run(f(dataAccess).transactionally.withTransactionIsolation(isolationLevel)), Duration.Inf))
  }

  def close(): Unit = {
    db.close()
  }
}
