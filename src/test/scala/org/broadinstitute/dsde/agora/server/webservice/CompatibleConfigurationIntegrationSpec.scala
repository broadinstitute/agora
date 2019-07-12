package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.{RouteTest, RouteTestTimeout, ScalatestRouteTest}
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.server.directives.ExecutionDirectives
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FreeSpec}

import scala.concurrent.duration._
import scala.util.Random

@DoNotDiscover
class CompatibleConfigurationIntegrationSpec extends FreeSpec with ExecutionDirectives with RouteTest
  with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  implicit val routeTestTimeout = RouteTestTimeout(20.seconds)

  trait ActorRefFactoryContext {
    def actorRefFactory: ActorSystem = system
  }

  val methodsService = new MethodsService(permsDataSource) with ActorRefFactoryContext
  val testRoutes = ApiService.handleExceptionsAndRejections (methodsService.queryCompatibleConfigurationsRoute)

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()

    // has no configurations
    patiently(agoraBusiness.insert(testMethod("A",
      Seq("in1"), Seq("out1")), mockAuthenticatedOwner.get))

    // one method snapshot, two compatible configs
    patiently(agoraBusiness.insert(testMethod("B",
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("B", 1,
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("B", 1,
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get))

    // two method snapshots, with one and two compatible configs
    patiently(agoraBusiness.insert(testMethod("C",
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testMethod("C",
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("C", 1,
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("C", 2,
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("C", 2,
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get))

    // one method snapshot, two compatible configs and one with incompatible inputs
    patiently(agoraBusiness.insert(testMethod("D",
      Seq("D1"), Seq("D2","D3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("D", 1,
      Seq("D1"), Seq("D2","D3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("D", 1,
      Seq("D1","incompatible"), Seq("D2","D3")), mockAuthenticatedOwner.get)) // <-- extra input
    patiently(agoraBusiness.insert(testConfig("D", 1,
      Seq("D1"), Seq("D2","D3")), mockAuthenticatedOwner.get))

    // one method snapshot, two compatible configs and one with incompatible outputs
    patiently(agoraBusiness.insert(testMethod("E",
      Seq("E1"), Seq("E2","E3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("E", 1,
      Seq("E1"), Seq("E2","E3")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("E", 1,
      Seq("E1"), Seq("E2")), mockAuthenticatedOwner.get)) // <-- missing output
    patiently(agoraBusiness.insert(testConfig("E", 1,
      Seq("E1"), Seq("E2","E3")), mockAuthenticatedOwner.get))

    // one method snapshot, one compatible config, but uses same ins/outs as "B"
    patiently(agoraBusiness.insert(testMethod("F",
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("F", 1,
      Seq("in1"), Seq("out1", "out2")), mockAuthenticatedOwner.get)) // <-- wrong inputs
    patiently(agoraBusiness.insert(testConfig("F", 1,
      Seq("in1","in2"), Seq("out1")), mockAuthenticatedOwner.get)) // <-- wrong outputs

    // method has optional inputs
    patiently(agoraBusiness.insert(testMethod("G",
      Seq("in1","in2"), Seq("out1", "out2"), Seq("optional1", "optional2")), mockAuthenticatedOwner.get))
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get)) // <-- all required, no optionals
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1","in2","optional1"), Seq("out1","out2")), mockAuthenticatedOwner.get)) // <-- all required, some optional
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1","in2","optional1","optional2"), Seq("out1","out2")), mockAuthenticatedOwner.get)) // <-- all required, all optional
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1", "optional1", "optional2"), Seq("out1","out2")), mockAuthenticatedOwner.get)) // <-- some required, all optional
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1", "in2", "in3"), Seq("out1","out2")), mockAuthenticatedOwner.get)) // <-- extraneous inputs

  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  "Agora's compatible configurations endpoint" - {
    "should reject any http method other than GET" in {
      val disallowedMethods = List(HttpMethods.POST, HttpMethods.PUT,
        HttpMethods.DELETE, HttpMethods.PATCH, HttpMethods.HEAD)

      disallowedMethods foreach {
        method =>
          new RequestBuilder(method)(testUrl("A", 1)) ~> testRoutes ~> check {
            assert(!handled)
          }
      }
    }
    "should return NotFound for a method snapshot that doesn't exist" in {
      Get(testUrl("not", "found", 1)) ~> testRoutes ~> check {
        assert(status == NotFound)
      }
    }
    "should return empty array for a method that has no configurations" in {
      Get(testUrl("A", 1)) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.isEmpty)
      }
    }
    "should return configs for a method that has compatible configurations" in {
      Get(testUrl("B", 1)) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.size == 2)
        configs.foreach { config =>
          validateConfig(config, "B", Seq("in1","in2"), Seq("out1","out2")) }
      }
    }

    "should return compatible configurations that reference any snapshot of this method" in {
      Seq(1,2) foreach { snapshotId =>
        Get(testUrl("C", snapshotId)) ~> testRoutes ~> check {
          assert(status == OK)
          val configs = responseAs[Seq[AgoraEntity]]
          assert(configs.size == 3)
          configs.foreach { config =>
            validateConfig(config, "C", Seq("in1","in2","in3"), Seq("out1","out2","out3")) }
        }
      }

    }
    "should omit configurations with different inputs" in {
      Get(testUrl("D", 1)) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.size == 2)
        configs.foreach { config =>
          validateConfig(config, "D", Seq("D1"), Seq("D2","D3")) }
      }
    }
    "should omit configurations with different outputs" in {
      Get(testUrl("E", 1)) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.size == 2)
        configs.foreach { config =>
          validateConfig(config, "E", Seq("E1"), Seq("E2","E3")) }
      }
    }
    "should omit compatible configurations that reference a different method" in {
      // also tests properly returning an empty array when the method
      // has only incompatible configs associated with it
      Get(testUrl("F", 1)) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.isEmpty)
      }
    }
    "should consider configurations compatible if they don't satisfy all optionals" in {
      Get(testUrl("G", 1)) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.size == 3)
        configs.foreach { config =>
          validateConfig(config, "G", Seq("in1","in2"), Seq("out1","out2"), allowOptionals = true) }
      }

    }
  }


  // =========================================================
  // =================== helper methods
  // =========================================================
  private def validateConfig(config:AgoraEntity, label:String, expectedInputs:Seq[String], expectedOutputs:Seq[String], allowOptionals:Boolean=false) = {
    assert(config.payloadObject.isDefined)
    assert(config.payloadObject.get.methodRepoMethod.methodName == s"name-$label")
    assert(config.payloadObject.get.methodRepoMethod.methodNamespace == s"namespace-$label")
    assert(config.payloadObject.get.outputs.keySet == expectedOutputs.map(out=>s"TheWorkflow.TheTask.$out").toSet)
    if (allowOptionals)
      assert((expectedInputs.map(in=>s"TheWorkflow.TheTask.$in").toSet diff config.payloadObject.get.inputs.keySet).isEmpty)
    else
      assert(config.payloadObject.get.inputs.keySet == expectedInputs.map(in=>s"TheWorkflow.TheTask.$in").toSet)

  }

  private def testUrl(namespace:String, name:String, snapshotId:Int): String =
    ApiUtil.Methods.withLeadingVersion + s"/$namespace/$name/$snapshotId/configurations"

  private def testUrl(label:String, snapshotId: Int): String =
    testUrl(s"namespace-$label", s"name-$label", snapshotId)

  private def testMethod(label:String, inputs:Seq[String], outputs:Seq[String], optionalInputs:Seq[String] = Seq.empty[String]): AgoraEntity =
    testIntegrationEntity.copy(namespace=Some(s"namespace-$label"),
      name=Some(s"name-$label"),
      payload=Some(makeWDL(inputs,outputs,optionalInputs)))

  private def testConfig(label:String, methodSnapshotId:Int, inputs:Seq[String], outputs:Seq[String]): AgoraEntity = {
    testAgoraConfigurationEntity.copy(
      namespace=Some(s"config-namespace-$label"),
      name=Some(s"config-name-$label"),
      payload=Some(makeConfig(label, methodSnapshotId, inputs, outputs))
    )
  }

  private def makeWDL(inputs:Seq[String], outputs:Seq[String], optionalInputs:Seq[String]) = {

    val inputWDL = (inputs map (in => s"$randType $in")).mkString("\n")
    val optionalInputWDL = (optionalInputs map (in => s"$randType? $in")).mkString("\n")
    val outputWDL = (outputs map (out => s"""$randType $out = "foo"""")).mkString("\n")

    val templateWDL = s"""task TheTask {
                        |  $inputWDL
                        |  $optionalInputWDL
                        |
                        |  command { foo }
                        |
                        |  output {
                        |	  $outputWDL
                        |  }
                        |}
                        |
                        |workflow TheWorkflow {
                        |  call TheTask
                        |}""".stripMargin

    templateWDL
  }

  private def randType:String = Random.shuffle(Seq("String","File")).head

  private def makeConfig(label:String, snapshotId:Int, inputs:Seq[String], outputs:Seq[String]) = {

    val inputConfig = (inputs map (in => s""" "TheWorkflow.TheTask.$in":"this.$in" """)).mkString(",\n")
    val outputConfig = (outputs map (out => s""" "TheWorkflow.TheTask.$out":"workspace.$out" """)).mkString(",\n")


    val templatePayload =
      s"""{

         |  "methodRepoMethod":{
         |    "methodNamespace":"namespace-$label",
         |    "methodName":"name-$label",
         |    "methodVersion":$snapshotId
         |  },
         |  "outputs":{
         |    $outputConfig
         |  },
         |  "inputs":{
         |    $inputConfig
         |  },
         |  "rootEntityType":"participant",
         |  "prerequisites":{},
         |  "methodConfigVersion":1,
         |  "deleted":false,
         |  "namespace":"config-namespace-$label",
         |  "name":"config-name-$label"
         |}""".stripMargin

    templatePayload
  }

}


