package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraAddRequest, AgoraEntity}

trait AgoraTestData {
  val namespace = "broad"

  val name = "testMethod"

  val synopsis = "This is a test method"

  val documentation = "This is the documentation"

  val owner = "bob the builder"

  val payload = """task wc {
                  |  command {
                  |    echo "${str}" | wc -c
                  |  }
                  |  output {
                  |    int count = read_int("stdout") - 1
                  |  }
                  |}
                  |
                  |workflow wf {
                  |  array[string] str_array
                  |  scatter(s in str_array) {
                  |    call wc{input: str=s}
                  |  }
                  |}""".stripMargin

  val testEntity = AgoraEntity(namespace = Option(namespace), name = Option(name))

  val testAddRequest = new AgoraAddRequest(
    namespace = namespace,
    name = name,
    synopsis = synopsis,
    documentation = documentation,
    owner = owner,
    payload = payload
  )

  val badPayload = "task test {"

  val testBadAddRequest = new AgoraAddRequest(
    namespace = namespace,
    name = name,
    synopsis = synopsis,
    documentation = documentation,
    owner = owner,
    payload = badPayload
  )
}
