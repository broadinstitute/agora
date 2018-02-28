package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.ga4gh.{Ga4ghService, ModelSupport}
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.{AgoraConfig, AgoraTestFixture}
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FreeSpecLike}
import spray.json._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import DefaultJsonProtocol._
import akka.http.scaladsl.server.directives.ExecutionDirectives

@DoNotDiscover
class Ga4ghServiceSpec extends FreeSpecLike with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture with ExecutionDirectives {

  trait ActorRefFactoryContext {
    def actorRefFactory: ActorSystem = system
  }

  private val ga4ghService = new Ga4ghService(permsDataSource) with ActorRefFactoryContext

  // these routes depend on the exception handler defined in ApiService, so
  // we have to add the exception handler back here.
  private val testRoutes = handleExceptions(ApiService.exceptionHandler) {
    ga4ghService.routes
  }

  private var agoraEntity1: AgoraEntity = _
  private var agoraEntity2: AgoraEntity = _
  private var agoraEntity2Snapshot: AgoraEntity = _
  private var agoraEntity3: AgoraEntity = _
  private var redactedEntity: AgoraEntity = _

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    // private method
    agoraEntity1 = patiently(agoraBusiness.insert(testIntegrationEntity, mockAuthenticatedOwner.get))

    // public method
    agoraEntity2 = patiently(agoraBusiness.insert(testIntegrationEntity2, owner2.get))
    patiently(permissionBusiness.insertEntityPermission(testIntegrationEntity2.copy(snapshotId = Some(1)), owner2.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))
    // additional non-public snapshot of this public method
    patiently(agoraBusiness.insert(testIntegrationEntity2, owner2.get))
    // additional public snapshot of this public method
    agoraEntity2Snapshot = patiently(agoraBusiness.insert(testIntegrationEntity2, owner2.get))
    patiently(permissionBusiness.insertEntityPermission(testIntegrationEntity2.copy(snapshotId = Some(3)), owner2.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))

    // redacted public method
    redactedEntity = patiently(agoraBusiness.insert(testEntityToBeRedacted2, mockAuthenticatedOwner.get))
    patiently(permissionBusiness.insertEntityPermission(testEntityToBeRedacted2.copy(snapshotId = Some(1)), mockAuthenticatedOwner.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))
    patiently(agoraBusiness.delete(redactedEntity, Seq(redactedEntity.entityType.get), mockAuthenticatedOwner.get))

    // another public method
    agoraEntity3 = patiently(agoraBusiness.insert(testIntegrationEntity3, owner2.get))
    patiently(permissionBusiness.insertEntityPermission(testIntegrationEntity3.copy(snapshotId = Some(1)), owner2.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))

  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  "Agora's GA4GH API" - {

    "List all tools endpoint" - {
      val endpoint = "/ga4gh/v1/tools"
      s"at $endpoint" - {
        testNonGet(endpoint)
        "should return a list of public Tools" in {
          Get(endpoint) ~> testRoutes ~> check {
            assert(status == OK)
            val actual = responseAs[Seq[Tool]]

            val method1Versions = List(
              ToolVersion(
                name = agoraEntity2.name.get,
                url = agoraEntity2.url.get,
                id = agoraEntity2.namespace.get + ":" + agoraEntity2.name.get,
                image = "",
                `descriptor-type` = List("WDL"),
                dockerfile = false,
                `meta-version` = agoraEntity2.snapshotId.get.toString,
                verified = false,
                `verified-source` = ""),
              ToolVersion(
                name = agoraEntity2Snapshot.name.get,
                url = agoraEntity2Snapshot.url.get,
                id = agoraEntity2Snapshot.namespace.get + ":" + agoraEntity2Snapshot.name.get,
                image = "",
                `descriptor-type` = List("WDL"),
                dockerfile = false,
                `meta-version` = agoraEntity2Snapshot.snapshotId.get.toString,
                verified = false,
                `verified-source` = ""))

            val method1 = Tool(
              url=AgoraConfig.GA4GH.toolUrl(agoraEntity2.namespace.get + ":" + agoraEntity2.name.get, method1Versions.last.id, method1Versions.last.`descriptor-type`.last),
              id=agoraEntity2.namespace.get + ":" + agoraEntity2.name.get,
              organization=ModelSupport.organization,
              toolname=agoraEntity2.name.get,
              toolclass=ToolClass("Workflow","Workflow",""),
              description=agoraEntity2.synopsis.get,
              author="",
              `meta-version`= method1Versions.last.`meta-version`,
              contains=List.empty[String],
              verified=false,
              `verified-source`="",
              signed=false,
              versions=method1Versions
            )

            val method2Versions = List(
              ToolVersion(
                name = agoraEntity3.name.get,
                url = agoraEntity3.url.get,
                id = agoraEntity3.namespace.get + ":" + agoraEntity3.name.get,
                image = "",
                `descriptor-type` = List("WDL"),
                dockerfile = false,
                `meta-version` = agoraEntity3.snapshotId.get.toString,
                verified = false,
                `verified-source` = ""))

            val method2 = Tool(
              url=AgoraConfig.GA4GH.toolUrl(agoraEntity3.namespace.get + ":" + agoraEntity3.name.get, method2Versions.last.id, method2Versions.last.`descriptor-type`.last),
              id=agoraEntity3.namespace.get + ":" + agoraEntity3.name.get,
              organization=ModelSupport.organization,
              toolname=agoraEntity3.name.get,
              toolclass=ToolClass("Workflow","Workflow",""),
              description=agoraEntity3.synopsis.get,
              author="",
              `meta-version` = method2Versions.last.`meta-version`,
              contains=List.empty[String],
              verified=false,
              `verified-source`="",
              signed=false,
              versions=method2Versions
            )

            val expected = Seq(method1, method2)

            assertResult(expected) { actual }
          }
        }
      }
    }

    "Single tool endpoint" - {
      val endpointTemplate = "/ga4gh/v1/tools/%s:%s"
      s"at $endpointTemplate" - {
        commonTests(endpointTemplate, runVersionTests = false, runDescriptorTypeTests = false)
        "should return a public Tool when asked, with associated ToolVersions" in {
          Get(fromTemplate(endpointTemplate)) ~> testRoutes ~> check {
            assert(status == OK)
            val actual = responseAs[Tool]

            val firstToolVersion = ToolVersion(
              name = agoraEntity2.name.get,
              url = agoraEntity2.url.get,
              id = agoraEntity2.namespace.get + ":" + agoraEntity2.name.get,
              image = "",
              `descriptor-type` = List("WDL"),
              dockerfile = false,
              `meta-version` = agoraEntity2.snapshotId.get.toString,
              verified = false,
              `verified-source` = "")
            val secondToolVersion = ToolVersion(
              name = agoraEntity2Snapshot.name.get,
              url = agoraEntity2Snapshot.url.get,
              id = agoraEntity2Snapshot.namespace.get + ":" + agoraEntity2Snapshot.name.get,
              image = "",
              `descriptor-type` = List("WDL"),
              dockerfile = false,
              `meta-version` = agoraEntity2Snapshot.snapshotId.get.toString,
              verified = false,
              `verified-source` = "")
            val expectedVersions = List(firstToolVersion, secondToolVersion)

            val expected = Tool(
              url=AgoraConfig.GA4GH.toolUrl(agoraEntity2.namespace.get + ":" + agoraEntity2.name.get, secondToolVersion.id, secondToolVersion.`descriptor-type`.last),
              id=agoraEntity2.namespace.get + ":" + agoraEntity2.name.get,
              organization=ModelSupport.organization,
              toolname=agoraEntity2.name.get,
              toolclass=ToolClass("Workflow","Workflow",""),
              description=agoraEntity2.synopsis.get,
              author="",
              `meta-version`=secondToolVersion.`meta-version`,
              contains=List.empty[String],
              verified=false,
              `verified-source`="",
              signed=false,
              versions=expectedVersions
            )

            assertResult(expected) { actual }
          }
        }
      }
    }

    "List all versions for tool endpoint" - {
      val endpointTemplate = "/ga4gh/v1/tools/%s:%s/versions"
      s"at $endpointTemplate" - {
        commonTests(endpointTemplate, runVersionTests = false, runDescriptorTypeTests = false)
        "should return a list of public ToolVersion when asked" in {
          Get(fromTemplate(endpointTemplate)) ~> testRoutes ~> check {
            assert(status == OK)
            val actual = responseAs[Seq[ToolVersion]]
            assert(actual.size == 2)
            val firstToolVersion = ToolVersion(
              name = agoraEntity2.name.get,
              url = agoraEntity2.url.get,
              id = agoraEntity2.namespace.get + ":" + agoraEntity2.name.get,
              image = "",
              `descriptor-type` = List("WDL"),
              dockerfile = false,
              `meta-version` = agoraEntity2.snapshotId.get.toString,
              verified = false,
              `verified-source` = "")
            val secondToolVersion = ToolVersion(
              name = agoraEntity2Snapshot.name.get,
              url = agoraEntity2Snapshot.url.get,
              id = agoraEntity2Snapshot.namespace.get + ":" + agoraEntity2Snapshot.name.get,
              image = "",
              `descriptor-type` = List("WDL"),
              dockerfile = false,
              `meta-version` = agoraEntity2Snapshot.snapshotId.get.toString,
              verified = false,
              `verified-source` = "")
            val expected = Set(firstToolVersion, secondToolVersion)

            assertResult(expected) { actual.toSet }
          }
        }
      }
    }

    "Single tool version endpoint" - {
      val endpointTemplate = "/ga4gh/v1/tools/%s:%s/versions/%d"
      s"at $endpointTemplate" - {
        commonTests(endpointTemplate, runDescriptorTypeTests = false)
        "should return a ToolVersion when asked for a public snapshot" in {
          Get(fromTemplate(endpointTemplate)) ~> testRoutes ~> check {
            assert(status == OK)
            val expected = ToolVersion(
              name = agoraEntity2.name.get,
              url = agoraEntity2.url.get,
              id = agoraEntity2.namespace.get + ":" + agoraEntity2.name.get,
              image = "",
              `descriptor-type` = List("WDL"),
              dockerfile = false,
              `meta-version` = agoraEntity2.snapshotId.get.toString,
              verified = false,
              `verified-source` = "")
            assertResult(expected) { responseAs[ToolVersion] }
          }
        }
      }
    }

    "Single tool version descriptor endpoint" - {
      val endpointTemplate = "/ga4gh/v1/tools/%s:%s/versions/%d/%s/descriptor"
      s"at $endpointTemplate" - {
        commonTests(endpointTemplate)
        "should return WDL plus metadata when asked for WDL of a public snapshot" in {
          Get(fromTemplate(endpointTemplate)) ~> testRoutes ~> check {
            assert(status == OK)
            val td = responseAs[ToolDescriptor]
            assert(td.`type` == ToolDescriptorType.WDL)
            assert(td.descriptor == testIntegrationEntity2.payload.get)
          }
        }
        "should return WDL only when asked for plain-WDL of a public snapshot" in {
          Get(fromTemplate(endpointTemplate, descriptorType = "plain-WDL")) ~> testRoutes ~> check {
            assert(status == OK)
            val td = responseAs[String]
            assert(td == testIntegrationEntity2.payload.get)
          }
        }
      }
    }

    "Unsupported/undocumented relative-path endpoint" - {
      val endpointTemplate = "/ga4gh/v1/tools/%s:%s/versions/%d/%s/descriptor/relative-path"
      s"at $endpointTemplate" - {
        testNonGet(fromTemplate(endpointTemplate))
        "should return NotImplemented when called" in {
          Get(fromTemplate(endpointTemplate)) ~> testRoutes ~> check {
            assert(status == NotImplemented)
          }
        }
      }
    }

    "Unsupported/undocumented tests endpoint" - {
      val endpointTemplate = "/ga4gh/v1/tools/%s:%s/versions/%d/$s/tests"
      s"at $endpointTemplate" - {
        testNonGet(fromTemplate(endpointTemplate))
        "should return NotImplemented when called" in {
          Get(fromTemplate(endpointTemplate)) ~> testRoutes ~> check {
            assert(status == NotImplemented)
          }
        }
      }
    }

    "Unsupported/undocumented single tool version dockerfile endpoint" - {
      val endpointTemplate = "/ga4gh/v1/tools/%s:%s/versions/%d/dockerfile"
      s"at $endpointTemplate" - {
        testNonGet(fromTemplate(endpointTemplate))
        "should return NotImplemented when called" in {
          Get(fromTemplate(endpointTemplate)) ~> testRoutes ~> check {
            assert(status == NotImplemented)
          }
        }
      }
    }

    "Metadata endpoint" - {
      val endpoint = "/ga4gh/v1/metadata"
      s"at $endpoint" - {
        testNonGet(endpoint)
        "should return expected metadata" in {
          val expected =
            """
              |{
              |  "version": "1.0.0",
              |  "api-version": "1.0.0",
              |  "country": "USA",
              |  "friendly-name": "FireCloud"
              |}
            """.stripMargin.parseJson
          Get(endpoint) ~> testRoutes ~> check {
            assert(status == OK)
            assertResult(expected) {
              responseAs[String].parseJson
            }
          }
        }
      }
    }

    "Tool-classes endpoint" - {
      val endpoint = "/ga4gh/v1/tool-classes"
      s"at $endpoint" - {
        testNonGet(endpoint)
        "should return expected tool classes" in {
          val expected = Seq(ToolClass("Workflow", "Workflow", ""))
          Get(endpoint) ~> testRoutes ~> check {
            assert(status == OK)
            assertResult(expected) {
              responseAs[Seq[ToolClass]]
            }
          }
        }
      }
    }

  }


  // =============== COMMON TESTS ===============

  private def commonTests(endpointTemplate: String,
                          runNamespaceTests: Boolean = true,
                          runVersionTests: Boolean = true,
                          runDescriptorTypeTests: Boolean = true): Unit = {
    lazy val endpoint = fromTemplate(endpointTemplate)
    testNonGet(endpoint)
    if (runNamespaceTests) commonNamespaceTests(endpoint)
    if (runVersionTests) commonVersionTests(endpointTemplate)
    if (runDescriptorTypeTests) commonDescriptorTypeTests(endpoint)
  }

  private def commonNamespaceTests(endpoint: => String): Unit = {
    "should return BadRequest when no colon in the namespace:name id" in {
      val mungedUrl = endpoint.replace(":", "")
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == BadRequest)
      }
    }
    "should return BadRequest when too many elements in the namespace:name id" in {
      val mungedUrl = endpoint.replace(":", ":more:")
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == BadRequest)
      }
    }
  }

  private def commonVersionTests(endpointTemplate: String): Unit = {
    lazy val endpoint = fromTemplate(endpointTemplate)
    "should return BadRequest when given a non-integer versionId" in {
      val mungedUrl = endpoint.replace("/1", "/notanumber")
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == BadRequest)
      }
    }
    "should return NotFound when asked for a method that doesn't exist" in {
      val mungedUrl = fromTemplate(endpointTemplate, namespace = "not", name = "found")
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == NotFound)
      }
    }
    "should return NotFound when asked for a snapshot that doesn't exist" in {
      val mungedUrl = fromTemplate(endpointTemplate, snapshotId = 123)
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == NotFound)
      }
    }
    "should return NotFound when asked for a private snapshot" in {
      // check that the test is set up correctly -
      // will throw an error if the entity doesn't exist in the db
      val privateEntity = patiently(agoraBusiness.findSingle(agoraEntity1, Seq(agoraEntity1.entityType.get), mockAuthenticatedOwner.get))
      val mungedUrl = fromTemplate(endpointTemplate,
        namespace = privateEntity.namespace.get, name = privateEntity.name.get, snapshotId = privateEntity.snapshotId.get)
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == NotFound)
      }
    }
    "should return NotFound when asked for a redacted snapshot" in {
      val mungedUrl = fromTemplate(endpointTemplate,
        namespace = redactedEntity.namespace.get, name = redactedEntity.name.get, snapshotId = redactedEntity.snapshotId.get)
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == NotFound)
      }
    }

  }

  private def commonDescriptorTypeTests(endpoint: => String): Unit = {
    "should return BadRequest when asked for CWL" in {
      val mungedUrl = endpoint.replace("WDL", "CWL")
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == BadRequest)
      }
    }
    "should return BadRequest when asked for plain-CWL" in {
      val mungedUrl = endpoint.replace("WDL", "plain-CWL")
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == BadRequest)
      }
    }
    "should return BadRequest when asked for an unknown type" in {
      val mungedUrl = endpoint.replace("WDL", "woodle")
      Get(mungedUrl) ~> testRoutes ~> check {
        assert(status == BadRequest)
      }
    }
  }

  private def testNonGet(endpoint: => String): Unit = {
    "should reject when asked for anything other than GET" in {
      val disallowedMethods = List(HttpMethods.POST, HttpMethods.PUT,
        HttpMethods.DELETE, HttpMethods.PATCH, HttpMethods.HEAD)

      disallowedMethods foreach {
        method =>
          new RequestBuilder(method)(endpoint) ~> testRoutes ~> check {
            assert(!handled)
          }
      }
    }
  }


  // =============== HELPER METHODS ===============

  private def fromTemplate(urlTemplate: String,
                           namespace: String = agoraEntity2.namespace.get,
                           name: String = agoraEntity2.name.get,
                           snapshotId: Int = agoraEntity2.snapshotId.get,
                           descriptorType: String = "WDL"): String =
    urlTemplate.format(namespace, name, snapshotId, descriptorType)

}
