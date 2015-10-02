package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}

object AgoraTestData {
  def getBigDocumentation: String = {
    // Read contents of a test markdown file into a single string.
    val markdown = io.Source.fromFile("src/test/resources/TESTMARKDOWN.md").getLines() mkString "\n"
    markdown * 7 // NB: File is 1.6 Kb, so 7* that is >10kb, our minimal required storage amount.
  }

  val agoraCIOwner = Option("agora-test")
  val namespace1 = Option("broad")
  val namespace2 = Option("hellbender")
  val namespace3 = Option("GATK")
  val name1 = Option("testMethod1")
  val name2 = Option("testMethod2")
  val name3 = Option("name3")
  val name4 = Option("name4")
  val snapshotId1 = Option(1)
  val snapshotId2 = Option(2)
  val nameNonExistent = Option("nonexistent")
  val synopsis1 = Option("This is a test method")
  val synopsis2 = Option("This is another test method")
  val synopsis3 = Option("This is a test configuration")
  val documentation1 = Option("This is the documentation")
  val documentation2 = Option("This is documentation for another method")

  val badNamespace = Option("    ")
  val badName = Option("   ")
  val badId = Option(-10)
  val badSynopsis = Option("a" * 81)

  val nameWc = Option("wc")
  val synopsisWc = Option("wc -- word, line, character, and byte count")
  val documentationWc = Option("This is the documentation for wc")

  // NB: save io by storing output.
  val bigDocumentation: Option[String] = Option(getBigDocumentation)
  val owner1 = Option("testowner1@broadinstitute.org")
  val owner2 = Option("testowner2@broadinstitute.org")
  val owner3 = Option("testowner3@broadinstitute.org")
  val adminUser = Option("admin@broadinstitute.org")
  val mockAutheticatedOwner = Option(AgoraConfig.mockAuthenticatedUserEmail)

  val payload1 = Option( """task grep {
                           |  String pattern
                           |  String? flags
                           |  File file_name
                           |
                           |  command {
                           |    grep ${pattern} ${flags} ${file_name}
                           |  }
                           |  output {
                           |    File out = "stdout"
                           |  }
                           |  runtime {
                           |    memory: "2 MB"
                           |    cpu: 1
                           |    defaultDisks: "mydisk 3 LOCAL_SSD"
                           |  }
                           |}
                           |
                           |task wc {
                           |  Array[File]+ files
                           |
                           |  command {
                           |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                           |  }
                           |  output {
                           |    Int count = read_int("stdout")
                           |  }
                           |}
                           |
                           |workflow scatter_gather_grep_wc {
                           |  Array[File] input_files
                           |
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
  val payload2 = Option("task test { command { test } }")
  val payloadWcTask = Option( """
                                |task wc {
                                |  Array[File]+ files
                                |
                                |  command {
                                |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
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
                                                   |  String pattern
                                                   |  String? flags
                                                   |  File file_name
                                                   |
                                                   |  command {
                                                   |    grep ${pattern} ${flags} ${file_name}
                                                   |  }
                                                   |  output {
                                                   |    File out = "stdout"
                                                   |  }
                                                   |  runtime {
                                                   |    memory: "2 MB"
                                                   |    cpu: 1
                                                   |    defaultDisks: "mydisk 3 LOCAL_SSD"
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
  val badPayloadInvalidImport = Option( """
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
  val badPayloadNonExistentImport = Option( """
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

  val taskConfigPayload = Option( s"""{
                                    |  "methodRepoMethod": {
                                    |    "methodNamespace": "${namespace1.get}",
                                    |    "methodName": "${name1.get}",
                                    |    "methodVersion": 1
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

  val taskConfigPayload2 = Option( s"""{
                                    |  "methodRepoMethod": {
                                    |    "methodNamespace": "${namespace2.get}",
                                    |    "methodName": "${name1.get}",
                                    |    "methodVersion": 1
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


  val taskConfigPayload3 = Option( s"""{
                                      |  "methodRepoMethod": {
                                      |    "methodNamespace": "${namespace3.get}",
                                      |    "methodName": "${name3.get}",
                                      |    "methodVersion": 1
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

  val payloadWithValidOfficialDockerImageInWdl = Option( """
                                    |task wc {
                                    |  Array[File]+ files
                                    |  command {
                                    |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                                    |  }
                                    |  output {
                                    |    Int count = read_int("stdout")
                                    |  }
                                    |  runtime {
                                    |    docker: "ubuntu:latest"
                                    |  }
                                    |}
                                    | """.stripMargin)
  val payloadWithInvalidOfficialDockerRepoNameInWdl = Option( """
                                                                |task wc {
                                                                |  Array[File]+ files
                                                                |  command {
                                                                |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                                                                |  }
                                                                |  output {
                                                                |    Int count = read_int("stdout")
                                                                |  }
                                                                |  runtime {
                                                                |    docker: "ubuntu_doesnotexist:latest"
                                                                |  }
                                                                |}
                                                                | """.stripMargin)
  val payloadWithInvalidOfficialDockerTagNameInWdl = Option( """
                                                             |task wc {
                                                             |  Array[File]+ files
                                                             |  command {
                                                             |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                                                             |  }
                                                             |  output {
                                                             |    Int count = read_int("stdout")
                                                             |  }
                                                             |  runtime {
                                                             |    docker: "ubuntu:ggrant_latest"
                                                             |  }
                                                             |}
                                                             | """.stripMargin)
  val payloadWithValidPersonalDockerImageInWdl = Option( """
                                                           |task wc {
                                                           |  Array[File]+ files
                                                           |  command {
                                                           |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                                                           |  }
                                                           |  output {
                                                           |    Int count = read_int("stdout")
                                                           |  }
                                                           |  runtime {
                                                           |    docker: "broadinstitute/scala-baseimage"
                                                           |  }
                                                           |}
                                                           | """.stripMargin)
  val payloadWithInvalidPersonalDockerUserNameInWdl = Option( """
                                                               |task wc {
                                                               |  Array[File]+ files
                                                               |  command {
                                                               |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                                                               |  }
                                                               |  output {
                                                               |    Int count = read_int("stdout")
                                                               |  }
                                                               |  runtime {
                                                               |    docker: "broadinstitute_doesnotexist/scala-baseimage:latest"
                                                               |    memory: "2 MB"
                                                               |    cpu: 1
                                                               |    defaultDisks: "mydisk 3 LOCAL_SSD"
                                                               |  }
                                                               |}
                                                               | """.stripMargin)
  val payloadWithInvalidPersonalDockerRepoNameInWdl = Option( """
                                                                |task wc {
                                                                |  Array[File]+ files
                                                                |  command {
                                                                |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                                                                |  }
                                                                |  output {
                                                                |    Int count = read_int("stdout")
                                                                |  }
                                                                |  runtime {
                                                                |    docker: "broadinstitute/scala-baseimage_doesnotexist:latest"
                                                                |    memory: "2 MB"
                                                                |    cpu: 1
                                                                |    defaultDisks: "mydisk 3 LOCAL_SSD"
                                                                |  }
                                                                |}
                                                                | """.stripMargin)
  val payloadWithInvalidPersonalDockerTagNameInWdl = Option( """
                                                                |task wc {
                                                                |  Array[File]+ files
                                                                |  command {
                                                                |    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
                                                                |  }
                                                                |  output {
                                                                |    Int count = read_int("stdout")
                                                                |  }
                                                                |  runtime {
                                                                |    docker: "broadinstitute/scala-baseimage:latest_doesnotexist"
                                                                |    memory: "2 MB"
                                                                |    cpu: 1
                                                                |    defaultDisks: "mydisk 3 LOCAL_SSD"
                                                                |  }
                                                                |}
                                                                | """.stripMargin)
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
    owner = owner2,
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

  val testEntityToBeRedacted = AgoraEntity(namespace = namespace1,
    name = name3,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner2,
    payload = payload1,
    entityType = Option(AgoraEntityType.Task)
  )

  val testEntityToBeRedacted2 = AgoraEntity(namespace = namespace3,
    name = name3,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task)
  )

  val testEntityToBeRedacted3 = AgoraEntity(namespace = namespace3,
    name = name4,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task)
  )

  val testEntityWithPublicPermissions = AgoraEntity(namespace = namespace1,
    name = name4,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task)
  )

  val testEntityTaskWc = AgoraEntity(namespace = namespace1,
    name = nameWc,
    synopsis = synopsisWc,
    documentation = documentationWc,
    owner = owner1,
    payload = payloadWcTask,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntity = new AgoraEntity(
    namespace = namespace3,
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

  val testAgoraEntityWithInvalidOfficialDockerImageInWdl = new AgoraEntity(namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payloadWithInvalidOfficialDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
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

  val testAgoraConfigurationEntity2 = new AgoraEntity(
    namespace = namespace3,
    name = name2,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload2,
    entityType = Option(AgoraEntityType.Configuration)
  )

  val testAgoraConfigurationEntity3 = new AgoraEntity(
    namespace = namespace2,
    name = name1,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload,
    entityType = Option(AgoraEntityType.Configuration)
  )

  val testAgoraConfigurationToBeRedacted = new AgoraEntity(
    namespace = namespace3,
    name = name3,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload3,
    entityType = Option(AgoraEntityType.Configuration)
  )
}
