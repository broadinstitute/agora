package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.AgoraEntity

trait AgoraTestData {
  def getBigDocumentation: String = {
    // Read contents of a test markdown file into a single string.
    val markdown = io.Source.fromFile("src/test/resources/TESTMARKDOWN.md").getLines() mkString "\n"
    markdown * 7  // NB: File is 1.6 Kb, so 7* that is >10kb, our minimal required storage amount.
  }

  val namespace1 = Option("broad")
  val namespace2 = Option("hellbender")
  val name1 = Option("testMethod1")
  val name2 = Option("testMethod2")
  val synopsis1 = Option("This is a test method")
  val synopsis2 = Option("This is another test method")
  val documentation1 = Option("This is the documentation")
  val documentation2 = Option("This is documentation for another method")
  // NB: save io by storing output.
  val bigDocumentation: Option[String] = Option(getBigDocumentation)
  val owner1 = Option("bob")
  val owner2 = Option("dave")
  val payload1 = Option("""task wc {
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
                   |}""".stripMargin)
  val payload2 = Option("task test {}")
  val badPayload = Option("task test {")

  val testEntity1 = AgoraEntity(namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1)

  val testEntity2 = AgoraEntity(namespace = namespace2,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1)

  val testEntity3 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1)

  val testEntity4 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload1)

  val testEntity5 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation2,
    owner = owner1,
    payload = payload1)

  val testEntity6 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner2,
    payload = payload1)

  val testEntity7 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload2)

  val testAgoraEntity = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1
  )

  val testBadAgoraEntity = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = badPayload
  )

  val testAgoraEntityBigDoc = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = bigDocumentation,
    owner = owner1,
    payload = payload1
  )
}