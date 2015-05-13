package org.broadinstitute.dsde.agora.server

import com.github.simplyscala.{MongoEmbedDatabase, MongodProps}
import org.broadinstitute.dsde.agora.server.dataaccess.AgoraDao
import org.broadinstitute.dsde.agora.server.dataaccess.mongo.MethodsDbTest
import org.broadinstitute.dsde.agora.server.webservice.ApiServiceSpec
import org.scalatest.{BeforeAndAfterAll, Suites}

trait AgoraDbTest {
  val agoraDao = AgoraDao.createAgoraDao
}

class AgoraTestSuite extends Suites(new ApiServiceSpec, new MethodsDbTest) with BeforeAndAfterAll with MongoEmbedDatabase {
  var mongoProps: MongodProps = null

  override def beforeAll() {
    println("Starting embedded mongo db instance.")
    mongoProps = mongoStart(port = AgoraConfig.mongoDbPort)
    println("Starting Agora web services.")
    Agora.start()
  }

  override def afterAll() {
    println("Stopping embedded mongo db instance.")
    mongoStop(mongoProps)
    println("Stopping Agora web services.")
    Agora.stop()
  }
}
