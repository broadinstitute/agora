package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraEntityType, AgoraEntity}

trait AgoraTestData {
  def getBigDocumentation: String = {
    // Read contents of a test markdown file into a single string.
    val markdown = io.Source.fromFile("src/test/resources/TESTMARKDOWN.md").getLines() mkString "\n"
    markdown * 7 // NB: File is 1.6 Kb, so 7* that is >10kb, our minimal required storage amount.
  }

  val agoraCIOwner = Option("agora-test")
  val namespace1 = Option("broad")
  val namespace2 = Option("hellbender")
  val name1 = Option("testMethod1")
  val name2 = Option("testMethod2")
  val nameNonExistent = Option("nonexistent")
  val synopsis1 = Option("This is a test method")
  val synopsis2 = Option("This is another test method")
  val synopsis3 = Option("This is a test configuration")
  val documentation1 = Option("This is the documentation")
  val documentation2 = Option("This is documentation for another method")

  val nameWc = Option("wc")
  val synopsisWc = Option("wc -- word, line, character, and byte count")
  val documentationWc = Option("This is the documentation for wc")

  // NB: save io by storing output.
  val bigDocumentation: Option[String] = Option(getBigDocumentation)
  val owner1 = Option("bob")
  val owner2 = Option("dave")
  val payload1 = Option( """task grep {
                           |  command {
                           |    grep ${pattern} ${flags?} ${File file_name}
                           |  }
                           |  output {
                           |    File out = "stdout"
                           |  }
                           |  runtime {
                           |    memory: "2MB"
                           |    cores: 1
                           |    disk: "3MB"
                           |  }
                           |}
                           |
                           |task wc {
                           |  command {
                           |    wc -l ${sep=' ' File files+} | tail -1 | cut -d' ' -f 2
                           |  }
                           |  output {
                           |    Int count = read_int("stdout")
                           |  }
                           |}
                           |
                           |workflow scatter_gather_grep_wc {
                           |  Array[File] input_files
                           |  scatter(f in input_files) {
                           |    call grep {
                           |      input: file_name = f
                           |    }
                           |  }
                           |  call wc {
                           |    input: files = grep.out
                           |  }
                           |}
                           |
                           |
                           | """.stripMargin)
  val payload2 = Option("task test {}")
  val payloadWcTask = Option("""
                           |task wc {
                           |  command {
                           |    wc -l ${sep=' ' File files+} | tail -1 | cut -d' ' -f 2
                           |  }
                           |  output {
                           |    Int count = read_int("stdout")
                           |  }
                           |}
                           |
                           | """.stripMargin)
  val payloadReferencingExternalMethod = Option( """
                           |import "methods://broad.wc.1"
                           |
                           |task grep {
                           |  command {
                           |    grep ${pattern} ${flags?} ${File file_name}
                           |  }
                           |  output {
                           |    File out = "stdout"
                           |  }
                           |  runtime {
                           |    memory: "2MB"
                           |    cores: 1
                           |    disk: "3MB"
                           |  }
                           |}
                           |
                           |workflow scatter_gather_grep_wc {
                           |  Array[File] input_files
                           |  scatter(f in input_files) {
                           |    call grep {
                           |      input: file_name = f
                           |    }
                           |  }
                           |  call wc {
                           |    input: files = grep.out
                           |  }
                           |}
                           | """.stripMargin)
  val badPayload = Option("task test {")
  val badPayloadInvalidImport = Option("""
                           |import "invalid_syntax_for_tool"
                           |import "broad.wc.1"
                           |
                           |workflow scatter_gather_grep_wc {
                           |  Array[File] input_files
                           |  scatter(f in input_files) {
                           |    call grep {
                           |      input: file_name = f
                           |    }
                           |  }
                           |  call wc {
                           |    input: files = grep.out
                           |  }
                           |}
                           |
                           | """.stripMargin)
  val badPayloadNonExistentImport = Option("""
                           |import "methods://broad.non_existent_grep.1"
                           |import "broad.wc.1"
                           |
                           |workflow scatter_gather_grep_wc {
                           |  Array[File] input_files
                           |  scatter(f in input_files) {
                           |    call grep {
                           |      input: file_name = f
                           |    }
                           |  }
                           |  call wc {
                           |    input: files = grep.out
                           |  }
                           |}
                           |
                           | """.stripMargin)

  val taskConfigPayload = Option("""{
                                   |  "method": {
                                   |    "name": "method",
                                   |    "namespace": "method_ns",
                                   |    "version": "1"
                                   |  },
                                   |  "name": "first",
                                   |  "workspaceName": {
                                   |    "namespace": "foo",
                                   |    "name": "bar"
                                   |  },
                                   |  "outputs": {
                                   |
                                   |  },
                                   |  "inputs": {
                                   |    "p": "hi"
                                   |  },
                                   |  "rootEntityType": "sample",
                                   |  "prerequisites": {
                                   |
                                   |  },
                                   |  "namespace": "ns"
                                   |}""".stripMargin)

  val testEntity1 = AgoraEntity(namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testEntity2 = AgoraEntity(namespace = namespace2,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testEntity3 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testEntity4 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testEntity5 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation2,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testEntity6 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner2,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testEntity7 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task))

  val testEntityTaskWc = AgoraEntity(namespace = namespace1,
    name = nameWc,
    synopsis = synopsisWc,
    documentation = documentationWc,
    owner = owner1,
    payload = payloadWcTask,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntity = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testBadAgoraEntity = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = badPayload,
    entityType = Option(AgoraEntityType.Task)
  )

  val testBadAgoraEntityInvalidWdlImportFormat = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = badPayloadInvalidImport,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testBadAgoraEntityNonExistentWdlImportFormat = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = badPayloadNonExistentImport,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testEntityWorkflowWithExistentWdlImport = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payloadReferencingExternalMethod,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testAgoraEntityBigDoc = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = bigDocumentation,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testAgoraEntityNonExistent = new AgoraEntity(
    namespace = namespace1,
    name = nameNonExistent,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow)
  )
  val testAgoraConfigurationEntity = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload,
    entityType = Option(AgoraEntityType.Configuration)
  )
}