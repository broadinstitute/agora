package org.broadinstitute.dsde.agora.server.dataaccess.permissions

import java.util.concurrent.{ExecutorService, Executors}

import org.broadinstitute.dsde.agora.server.dataaccess.ReadWriteAction
import org.broadinstitute.dsde.agora.server.AgoraConfig.EnhancedScalaConfig
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcProfile, TransactionIsolation}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class DataAccess(profile: JdbcProfile) {
  val nsPerms = new NamespacePermissionsClient(profile)
  val aePerms = new AgoraEntityPermissionsClient(profile)
}

class PermissionsDataSource(databaseConfig: DatabaseConfig[JdbcProfile]) {
  val profile = databaseConfig.profile
  val db = databaseConfig.db

  import profile.api._

  val dataAccess = new DataAccess(profile)

  private val actionThreadPool: ExecutorService = {
    val dbNumThreads = databaseConfig.config.getIntOr("db.numThreads", 20)
    val dbMaximumPoolSize = databaseConfig.config.getIntOr("db.maxConnections", dbNumThreads * 5)
    val actionThreadPoolSize = databaseConfig.config.getIntOr("actionThreadPoolSize", dbNumThreads) min dbMaximumPoolSize
    Executors.newFixedThreadPool(actionThreadPoolSize)
  }
  private val actionExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(
    actionThreadPool, db.executor.executionContext.reportFailure)

  def inTransaction[T](f: (DataAccess) => ReadWriteAction[T], isolationLevel: TransactionIsolation = TransactionIsolation.RepeatableRead): Future[T] = {
    Future(Await.result(db.run(f(dataAccess).transactionally.withTransactionIsolation(isolationLevel)), Duration.Inf))(actionExecutionContext)
  }

  def close(): Unit = {
    db.close()
  }
}
