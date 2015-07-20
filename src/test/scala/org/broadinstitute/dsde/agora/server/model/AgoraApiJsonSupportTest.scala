package org.broadinstitute.dsde.agora.server.model

import org.broadinstitute.dsde.agora.server.model.AgoraApiJsonSupport.UserInfoResponseFormat
import org.broadinstitute.dsde.agora.server.webservice.util.AgoraOpenAMClient.UserInfoResponse
import org.scalatest.{DoNotDiscover, FlatSpec}
import spray.json._

@DoNotDiscover
class AgoraApiJsonSupportTest extends FlatSpec with DefaultJsonProtocol {

  "AgoraApiJsonSupport" should "convert UserInfoResponse to JSON and back when mail is not empty" in {
    val userInfo = UserInfoResponse("user1", Seq("cn1", "cn2"), Seq("email1", "email2"))
    val userInfoFromRead = UserInfoResponseFormat.read(userInfo.toJson)
    assert(userInfoFromRead.username === "user1")
    assert(userInfoFromRead.cn === Seq("cn1", "cn2"))
    assert(userInfoFromRead.mail === Seq("email1", "email2"))
  }

  "AgoraApiJsonSupport" should "convert UserInfoResponse to JSON and back when mail is empty" in {
    val userInfo = UserInfoResponse("user1", Seq("cn1", "cn2"), Seq())
    val userInfoFromRead = UserInfoResponseFormat.read(userInfo.toJson)
    assert(userInfoFromRead.username === "user1")
    assert(userInfoFromRead.cn === Seq("cn1", "cn2"))
    assert(userInfoFromRead.mail === Seq())
  }

}
