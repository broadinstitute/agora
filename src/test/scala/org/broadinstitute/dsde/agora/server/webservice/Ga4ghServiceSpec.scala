package org.broadinstitute.dsde.agora.server.webservice

import akka.actor.ActorSystem
import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.AgoraTestFixture
import org.broadinstitute.dsde.agora.server.dataaccess.permissions.{AccessControl, AgoraPermissions}
import org.broadinstitute.dsde.agora.server.ga4gh.Ga4ghService
import org.broadinstitute.dsde.agora.server.ga4gh.Models._
import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FreeSpecLike}
import spray.http.HttpMethods
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.testkit.{RouteTest, ScalatestRouteTest}

@DoNotDiscover
class Ga4ghServiceSpec extends ApiServiceSpec with FreeSpecLike with RouteTest with ScalatestRouteTest with BeforeAndAfterAll with AgoraTestFixture {

  trait ActorRefFactoryContext {
    def actorRefFactory: ActorSystem = system
  }

  val ga4ghService = new Ga4ghService(permsDataSource) with ActorRefFactoryContext

  // these routes depend on the exception handler defined in ApiServiceActor, so
  // we have to add the exception handler back here.
  val testRoutes = wrapWithExceptionHandler{ga4ghService.routes}

  var agoraEntity1: AgoraEntity = _
  var agoraEntity2: AgoraEntity = _
  var redactedEntity: AgoraEntity = _

  override def beforeAll(): Unit = {
    ensureDatabasesAreRunning()
    // private method
    agoraEntity1 = patiently(agoraBusiness.insert(testIntegrationEntity, mockAuthenticatedOwner.get))
    // public method
    agoraEntity2 = patiently(agoraBusiness.insert(testIntegrationEntity2, owner2.get))
    patiently(permissionBusiness.insertEntityPermission(testIntegrationEntity2.copy(snapshotId = Some(1)), owner2.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))
    // redacted public method
    redactedEntity = patiently(agoraBusiness.insert(testEntityToBeRedacted2, mockAuthenticatedOwner.get))
    patiently(permissionBusiness.insertEntityPermission(testEntityToBeRedacted2.copy(snapshotId = Some(1)), mockAuthenticatedOwner.get,
      AccessControl(AccessControl.publicUser, AgoraPermissions(AgoraPermissions.Read))))
    patiently(agoraBusiness.delete(redactedEntity, Seq(redactedEntity.entityType.get), mockAuthenticatedOwner.get))
  }

  override def afterAll(): Unit = {
    clearDatabases()
  }

  "Agora's GA4GH API" - {
    "Tool descriptor endpoint" - {
      "should return WDL plus metadata when asked for WDL of a public snapshot" in {
        Get(defaultTestUrl()) ~> testRoutes ~> check {
          assert(status == OK)
          val td = responseAs[ToolDescriptor]
          assert(td.`type` == ToolDescriptorType.WDL)
          assert(td.descriptor == testIntegrationEntity2.payload.get)
        }
      }
      "should return WDL only when asked for plain-WDL of a public snapshot" in {
        Get(defaultTestUrl(descriptorType="plain-WDL")) ~> testRoutes ~> check {
          assert(status == OK)
          val td = responseAs[String]
          assert(td == testIntegrationEntity2.payload.get)
        }
      }
      "should return BadRequest when given a non-integer versionId" in {
        val mungedUrl = defaultTestUrl().replace("/1", "/notanumber")
        Get(mungedUrl) ~> testRoutes ~> check {
          assert(status == BadRequest)
        }
      }
      "should return BadRequest when no colon in the namespace:name id" in {
        val mungedUrl = defaultTestUrl().replace(":", "")
        Get(mungedUrl) ~> testRoutes ~> check {
          assert(status == BadRequest)
        }
      }
      "should return BadRequest when too many elements in the namespace:name id" in {
        val mungedUrl = defaultTestUrl().replace("testWorkflow", "testWorkflow:more:evenmore")
        Get(mungedUrl) ~> testRoutes ~> check {
          assert(status == BadRequest)
        }
      }
      "should return BadRequest when asked for CWL" in {
        Get(defaultTestUrl(descriptorType="CWL")) ~> testRoutes ~> check {
          assert(status == BadRequest)
        }
      }
      "should return BadRequest when asked for plain-CWL" in {
        Get(defaultTestUrl(descriptorType="plain-CWL")) ~> testRoutes ~> check {
          assert(status == BadRequest)
        }
      }
      "should return BadRequest when asked for an unknown type" in {
        Get(defaultTestUrl(descriptorType="woodle")) ~> testRoutes ~> check {
          assert(status == BadRequest)
        }
      }
      "should return NotFound when asked for a method that doesn't exist" in {
        Get(testUrl("not","found",1,"WDL")) ~> testRoutes ~> check {
          assert(status == NotFound)
        }
      }
      "should return NotFound when asked for a snapshot that doesn't exist" in {
        Get(defaultTestUrl(snapshotId=123)) ~> testRoutes ~> check {
          assert(status == NotFound)
        }
      }
      "should return NotFound when asked for a private snapshot" in {
        // check that the test is set up correctly -
        // will throw an error if the entity doesn't exist in the db
        val privateEntity = patiently(agoraBusiness.findSingle(agoraEntity1, Seq(agoraEntity1.entityType.get), mockAuthenticatedOwner.get))

        Get(testUrl(agoraEntity1)) ~> testRoutes ~> check {
          assert(status == NotFound)
        }
      }
      "should return NotFound when asked for a redacted snapshot" in {
        Get(testUrl(redactedEntity)) ~> testRoutes ~> check {
          assert(status == NotFound)
        }
      }
      "should reject when asked for anything other than GET" in {
        val disallowedMethods = List(HttpMethods.POST, HttpMethods.PUT,
          HttpMethods.DELETE, HttpMethods.PATCH, HttpMethods.HEAD)

        disallowedMethods foreach {
          method =>
            new RequestBuilder(method)(defaultTestUrl()) ~> testRoutes ~> check {
              assert(!handled)
            }
        }
      }
    }
  }


  private def testUrl(namespace:String, name:String, snapshotId:Int, descriptorType:String): String =
    s"/ga4gh/v1/tools/$namespace:$name/versions/$snapshotId/$descriptorType/descriptor"

  private def testUrl(entity:AgoraEntity): String =
    testUrl(entity.namespace.get, entity.name.get, entity.snapshotId.get, "WDL")

  // this is the known-good public snapshot
  private def defaultTestUrl(snapshotId:Int=agoraEntity2.snapshotId.get, descriptorType:String="WDL"): String =
    testUrl(agoraEntity2.namespace.get, agoraEntity2.name.get, snapshotId, descriptorType)

}
