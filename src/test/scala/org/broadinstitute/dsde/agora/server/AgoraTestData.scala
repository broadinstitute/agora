package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.business.AgoraBusiness
import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}

object AgoraTestData {
  def getBigDocumentation: String = {
    // Read contents of a test markdown file into a single string.
    val markdown = io.Source.fromFile("src/test/resources/TESTMARKDOWN.md").getLines() mkString "\n"
    markdown * 7 // NB: File is 1.6 Kb, so 7* that is >10kb, our minimal required storage amount.
  }

  val agoraTestOwner = Option("agora-test")
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
  val taskNamespace2 = Option("taskNamespace2")
  val taskName2 = Option("taskName2")

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

  val badConfigPayloadReferencesTask = Option( s"""{
                                      |  "methodRepoMethod": {
                                      |    "methodNamespace": "${taskNamespace2.get}",
                                      |    "methodName": "${taskName2.get}",
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
  val testWorkflow1 = AgoraEntity(namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testWorkflow2 = AgoraEntity(namespace = namespace2,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner2,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testWorkflow3 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testWorkflow4 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testWorkflow5 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation2,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testWorkflow6 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner2,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testWorkflow7 = new AgoraEntity(
    namespace = namespace3,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testTask1 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task))

  val testTask2 = new AgoraEntity(
    namespace = taskNamespace2,
    name = taskName2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskToBeRedacted1 = AgoraEntity(namespace = namespace1,
    name = name3,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner2,
    payload = payload1,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskToBeRedacted2 = AgoraEntity(namespace = namespace3,
    name = name4,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task)
  )

  val testWorkflowToBeRedacted = AgoraEntity(namespace = namespace3,
    name = name3,
    synopsis = synopsis2,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testTaskWithPublicPermissions = AgoraEntity(namespace = namespace1,
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

  val testBadAgoraTask = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = badPayload,
    entityType = Option(AgoraEntityType.Task)
  )

  val testBadWorkflowInvalidWdlImportFormat = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = badPayloadInvalidImport,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testBadWorkflowNonExistentWdlImportFormat = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = badPayloadNonExistentImport,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testTaskWithInvalidOfficialDockerImageInWdl = new AgoraEntity(namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payloadWithInvalidOfficialDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testWorkflowWithExistentWdlImport = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payloadReferencingExternalMethod,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testNonExistentWorkflow = new AgoraEntity(
    namespace = namespace1,
    name = nameNonExistent,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val testConfig1 = new AgoraEntity(
    namespace = namespace1,
    name = name1,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload,
    entityType = Option(AgoraEntityType.Configuration)
  )

  val testConfig2 = new AgoraEntity(
    namespace = namespace3,
    name = name2,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload2,
    entityType = Option(AgoraEntityType.Configuration)
  )

  val config3Namespace = Option("config3Namespace")
  val config3Name = Option("config3Name")
  val testConfig3 = new AgoraEntity(
    namespace = config3Namespace,
    name = config3Name,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload,
    entityType = Option(AgoraEntityType.Configuration)
  )

  val testConfigToBeRedacted = new AgoraEntity(
    namespace = namespace3,
    name = name3,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = taskConfigPayload3,
    entityType = Option(AgoraEntityType.Configuration)
  )

  val testBadConfigNamespace = Option("badConfigNamespace")
  val testBadConfigName = Option("badConfigName")
  val testBadConfigReferencesTask = new AgoraEntity(
    namespace = testBadConfigNamespace,
    name = testBadConfigName,
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner1,
    payload = badConfigPayloadReferencesTask,
    entityType = Option(AgoraEntityType.Configuration)
  )

  val testIntegrationWorkflow = AgoraEntity(namespace = Option("___test1"),
    name = Option("testWorkflow"),
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testIntegrationWorkflow2 = AgoraEntity(namespace = Option("___test2"),
    name = Option("testWorkflow"),
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner2,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testTaskWithValidOfficialDockerImageInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payloadWithValidOfficialDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskWithInvalidOfficialDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payloadWithInvalidOfficialDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskWithInvalidOfficialDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payloadWithInvalidOfficialDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskWithValidPersonalDockerInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payloadWithValidPersonalDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskWithInvalidPersonalDockerUserNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payloadWithInvalidPersonalDockerUserNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskWithInvalidPersonalDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payloadWithInvalidPersonalDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testTaskWithInvalidPersonalDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAutheticatedOwner,
    payload = payloadWithInvalidPersonalDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )
}
