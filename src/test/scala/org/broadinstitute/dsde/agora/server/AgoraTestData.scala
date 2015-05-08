package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraAddRequest, AgoraEntity}

trait AgoraTestData {
  def getBigDocumentation: String = {
    // Read contents of a test markdown file into a single string.
    val markdown = io.Source.fromFile("src/test/resources/TESTMARKDOWN.md").getLines() mkString "\n"
    markdown * 7  // NB: File is 1.6 Kb, so 7* that is >10kb, our minimal required storage amount.
  }
  
  val namespace1 = "broad"
  val namespace2 = "hellbender"
  val name1 = "testMethod1"
  val name2 = "testMethod2"
  val synopsis = "This is a test method"
  val documentation = "This is the documentation"
  // NB: save io by storing output.
  val bigDocumentation: String = getBigDocumentation
  val owner1 = "bob"
  val owner2 = "dave"
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

  val testEntity1 = AgoraEntity(namespace = Option(namespace1), name = Option(name1), owner = Option(owner1))
  val testEntity2 = AgoraEntity(namespace = Option(namespace1), name = Option(name2), owner = Option(owner1))
  val testEntity3 = AgoraEntity(namespace = Option(namespace2), name = Option(name1), owner = Option(owner1))
  val testEntity4 = AgoraEntity(namespace = Option(namespace1), name = Option(name1), owner = Option(owner2))

  val testAddRequest = new AgoraAddRequest(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis,
    documentation = documentation,
    owner = owner1,
    payload = payload
  )

  val badPayload = "task test {"

  val testBadAddRequest = new AgoraAddRequest(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis,
    documentation = documentation,
    owner = owner1,
    payload = badPayload
  )

  val testAddRequestBigDoc = new AgoraAddRequest(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis,
    documentation = bigDocumentation,
    owner = owner1,
    payload = payload
  )
}
