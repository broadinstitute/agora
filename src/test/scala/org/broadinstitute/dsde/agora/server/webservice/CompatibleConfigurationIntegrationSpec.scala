package org.broadinstitute.dsde.agora.server.webservice

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.{RouteTest, RouteTestTimeout, ScalatestRouteTest}
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ExecutionDirectives
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.webservice.methods.MethodsService
import org.broadinstitute.dsde.agora.server.webservice.routes.MockAgoraDirectives
import org.broadinstitute.dsde.agora.server.webservice.util.ApiUtil
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FreeSpec}

import scala.concurrent.duration._

@DoNotDiscover
class CompatibleConfigurationIntegrationSpec extends FreeSpec with ExecutionDirectives with RouteTest
  with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  implicit val routeTestTimeout: RouteTestTimeout = RouteTestTimeout(20.seconds)

  val methodsService = new MethodsService(permsDataSource)
  val testRoutes: Route = ApiService.handleExceptionsAndRejections (methodsService.queryCompatibleConfigurationsRoute)

  // The following methods create a method AgoraEntity while at the same time constructing the Mock Waas
  // describe response.  This test is more difficult than the others because it generates many unique
  // method payloads (WDLs) so it can test the compatibility of methods and we define "compatibility"
  // in this context as having matching inputs/outputs, which requires WaaS parsing!
  private def insertMethodWithWaasResponse(label:String, inputs:Seq[String], outputs:Seq[String], username: String, accessToken: String, optionalInputs:Seq[String] =  Seq.empty[String]) = {
    val method = testMethod(label, inputs, outputs, optionalInputs)
    addSubstringResponse(method.payload.get, waasResponse(inputs, outputs, optionalInputs))
    agoraBusiness.insert(method, username, accessToken)
  }

  private def waasResponse(inputs:Seq[String], outputs:Seq[String], optionalInputs:Seq[String]) = {
    val requiredInputStanzas = inputs map (generateInputStanza(_))
    val optionalInputStanzas = optionalInputs map (generateInputStanza(_, optional = true))
    val inputString = (requiredInputStanzas ++ optionalInputStanzas).mkString(",")
    val outputString = (outputs map (x => generateOutputStanza(x))).mkString(",")

    s"""
       | {"valid":true,"errors":[],"validWorkflow":true,"name":"TheWorkflow",
       |  "inputs":[$inputString],
       |  "outputs":[$outputString],
       |"images":[],
       |"submittedDescriptorType":{"descriptorType":"WDL","descriptorTypeVersion":"draft-2"},
       |"importedDescriptorTypes":[],
       |"meta":{},
       |"parameterMeta":{}
       |}
       |
     """.stripMargin
  }

  private def generateInputStanza(name : String, optional : Boolean = false) = {
    val optionalSuffix = if (optional) "?" else ""
    s"""
       |{
       |  "name":"TheTask.$name",
       |  "valueType":{"typeName":"File"},
       |  "typeDisplayName":"File$optionalSuffix",
       |  "optional":$optional,
       |  "default":null
       |}
     """.stripMargin
  }

  private def generateOutputStanza(name : String) = {
    s"""
       |{"name":"TheTask.$name","valueType":{"typeName":"File"},"typeDisplayName":"File"}
     """.stripMargin
  }

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    startMockWaas()

    // has no configurations
    patiently(insertMethodWithWaasResponse("A",
      Seq("in1"), Seq("out1"), mockAuthenticatedOwner.get, mockAccessToken))

    // one method snapshot, two compatible configs
    patiently(insertMethodWithWaasResponse("B",
      Seq("in1","in2"), Seq("out1", "out2"), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("B", 1,
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("B", 1,
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get, mockAccessToken))

    // two method snapshots, with one and two compatible configs
    patiently(insertMethodWithWaasResponse("C",
      Seq("in1","in2","in3"), Seq("out1", "out2","out3"), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(insertMethodWithWaasResponse("C",
      Seq("in1","in2","in3"), Seq("out1", "out2","out3"), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("C", 1,
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("C", 2,
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("C", 2,
      Seq("in1","in2","in3"), Seq("out1", "out2","out3")), mockAuthenticatedOwner.get, mockAccessToken))

    // one method snapshot, two compatible configs and one with incompatible inputs
    patiently(insertMethodWithWaasResponse("D",
      Seq("D1"), Seq("D2","D3"), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("D", 1,
      Seq("D1"), Seq("D2","D3")), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("D", 1,
      Seq("D1","incompatible"), Seq("D2","D3")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- extra input
    patiently(agoraBusiness.insert(testConfig("D", 1,
      Seq("D1"), Seq("D2","D3")), mockAuthenticatedOwner.get, mockAccessToken))

    // one method snapshot, two compatible configs and one with incompatible outputs
    patiently(insertMethodWithWaasResponse("E",
      Seq("E1"), Seq("E2","E3"), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("E", 1,
      Seq("E1"), Seq("E2","E3")), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("E", 1,
      Seq("E1"), Seq("E2")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- missing output
    patiently(agoraBusiness.insert(testConfig("E", 1,
      Seq("E1"), Seq("E2","E3")), mockAuthenticatedOwner.get, mockAccessToken))

    // one method snapshot, one compatible config, but uses same ins/outs as "B"
    patiently(insertMethodWithWaasResponse("F",
      Seq("in1","in2"), Seq("out1", "out2"), mockAuthenticatedOwner.get, mockAccessToken))
    patiently(agoraBusiness.insert(testConfig("F", 1,
      Seq("in1"), Seq("out1", "out2")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- wrong inputs
    patiently(agoraBusiness.insert(testConfig("F", 1,
      Seq("in1","in2"), Seq("out1")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- wrong outputs

    // method has optional inputs
    patiently(insertMethodWithWaasResponse("G",
      Seq("in1","in2"), Seq("out1", "out2"), mockAuthenticatedOwner.get, mockAccessToken, Seq("optional1", "optional2")))
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1","in2"), Seq("out1", "out2")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- all required, no optionals
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1","in2","optional1"), Seq("out1","out2")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- all required, some optional
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1","in2","optional1","optional2"), Seq("out1","out2")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- all required, all optional
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1", "optional1", "optional2"), Seq("out1","out2")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- some required, all optional
    patiently(agoraBusiness.insert(testConfig("G", 1,
      Seq("in1", "in2", "in3"), Seq("out1","out2")), mockAuthenticatedOwner.get, mockAccessToken)) // <-- extraneous inputs

  }

  override def afterAll(): Unit = {
    clearDatabases()
    stopMockWaas()
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
      Get(testUrl("not", "found", 1)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
        assert(status == NotFound)
      }
    }
    "should return empty array for a method that has no configurations" in {
      Get(testUrl("A", 1)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.isEmpty)
      }
    }
    "should return configs for a method that has compatible configurations" in {
      Get(testUrl("B", 1)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.size == 2)
        configs.foreach { config =>
          validateConfig(config, "B", Seq("in1","in2"), Seq("out1","out2")) }
      }
    }

    "should return compatible configurations that reference any snapshot of this method" in {
      Seq(1,2) foreach { snapshotId =>
        Get(testUrl("C", snapshotId)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
          assert(status == OK)
          val configs = responseAs[Seq[AgoraEntity]]
          assert(configs.size == 3)
          configs.foreach { config =>
            validateConfig(config, "C", Seq("in1","in2","in3"), Seq("out1","out2","out3")) }
        }
      }

    }
    "should omit configurations with different inputs" in {
      Get(testUrl("D", 1)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.size == 2)
        configs.foreach { config =>
          validateConfig(config, "D", Seq("D1"), Seq("D2","D3")) }
      }
    }
    "should omit configurations with different outputs" in {
      Get(testUrl("E", 1)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
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
      Get(testUrl("F", 1)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
        assert(status == OK)
        val configs = responseAs[Seq[AgoraEntity]]
        assert(configs.isEmpty)
      }
    }
    "should consider configurations compatible if they don't satisfy all optionals" in {
      Get(testUrl("G", 1)) ~> addHeader(MockAgoraDirectives.mockAccessToken, mockAccessToken) ~> testRoutes ~> check {
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

  private def testMethod(label:String, inputs:Seq[String], outputs:Seq[String], optionalInputs:Seq[String]): AgoraEntity =
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

    val inputWDL = (inputs map (in => s"File $in")).mkString("\n")
    val optionalInputWDL = (optionalInputs map (in => s"File? $in")).mkString("\n")
    val outputWDL = (outputs map (out => s"""File $out = "foo"""")).mkString("\n")

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


