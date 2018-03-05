package org.broadinstitute.dsde.agora.server

import org.broadinstitute.dsde.agora.server.model.{AgoraEntity, AgoraEntityType}

import scala.io.Source

object AgoraTestData {
  def getBigDocumentation: String = {
    // Read contents of a test markdown file into a single string.
    val markdown = Source.fromFile("src/test/resources/TESTMARKDOWN.md").getLines() mkString "\n"
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
  val name5 = Option("cancer_exome_pipeline_v2")
  val nameWithAllLegalChars = Option("abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ_0123456789.")
  val snapshotId1 = Option(1)
  val snapshotComment1 = Option("snapshot comment 1")
  val snapshotComment2 = Option("snapshot comment 2")
  val nameNonExistent = Option("nonexistent")
  val synopsis1 = Option("This is a test method")
  val synopsis2 = Option("This is another test method")
  val synopsis3 = Option("This is a test configuration")
  val documentation1 = Option("This is the documentation")
  val documentation2 = Option("This is documentation for another method")
  val testGA4GHpath = "/ga4gh/v1/tools/test_ns:test_wdl/versions/1/WDL/descriptor"
  val mockServerPort = 8001

  val badNamespace = Option("    ")
  val badName = Option("   ")
  val badNameWithIllegalChars = Option("does it work?")
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
  val mockAuthenticatedOwner = Option(AgoraConfig.mockAuthenticatedUserEmail)

  val fillerText = "activated charcoal palo santo, occupy listicle quinoa scenester next level kitsch gentrify XOXO tumeric everyday carry"

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

  val testAgoraEntity = new AgoraEntity(
    namespace = namespace3,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow)
  )

  val payloadReferencingExternalMethod = Option( s"""
                                                   |import "http://localhost:$mockServerPort$testGA4GHpath"
                                                   |
                                                   |task grep {
                                                   |  String pattern
                                                   |  String? flags
                                                   |  File file_name
                                                   |
                                                   |  command {
                                                   |    grep $${pattern} $${flags} $${file_name}
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
                                                   |}
                                                   | """.stripMargin)

  val payloadReferencingExternalMethodNotFound = Option( s"""
                                                    |import "http://localhost:$mockServerPort/not-found"
                                                    |
                                                    |task grep {
                                                    |  String pattern
                                                    |  String? flags
                                                    |  File file_name
                                                    |
                                                    |  command {
                                                    |    grep $${pattern} $${flags} $${file_name}
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
                                              |import "broad.non_existent_grep.1"
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

  val methodConfigPayload = Some(s"""
                              |{
                              |  "name": "${name5.get}",
                              |  "methodRepoMethod": {
                              |    "methodNamespace": "${namespace1.get}",
                              |    "methodName": "${name1.get}",
                              |    "methodVersion": 1
                              |  },
                              |  "outputs": {
                              |    "CancerExomePipeline_v2.M2.m2_output_vcf": "this.OUTPUT_VCF"
                              |  },
                              |  "inputs": {
                              |    "CancerExomePipeline_v2.M2.intervals": "this.ref_intervals",
                              |    "CancerExomePipeline_v2.M2.tumor_bam": "this.case_bam",
                              |    "CancerExomePipeline_v2.M2.normal_bam": "this.control_bam",
                              |    "CancerExomePipeline_v2.M2.ref_fasta_dict": "this.ref_dict",
                              |    "CancerExomePipeline_v2.M2.tumor_bai": "this.case_bai",
                              |    "CancerExomePipeline_v2.M2.normal_bai": "this.control_bai",
                              |    "CancerExomePipeline_v2.M2.ref_fasta": "this.ref_fasta",
                              |    "CancerExomePipeline_v2.M2.ref_fasta_fai": "this.ref_fai",
                              |    "CancerExomePipeline_v2.M2.m2_output_vcf_name": "this.vcf_output_name"
                              |  },
                              |  "rootEntityType": "pair",
                              |  "prerequisites": {},
                              |  "methodConfigVersion": 1,
                              |  "deleted": false,
                              |  "namespace": "${namespace1.get}"
                              |}""".stripMargin)

  val methodConfigPayloadMissingConfigVersion = Some(s"""
                                    |{
                                    |  "name": "${name5.get}",
                                    |  "methodRepoMethod": {
                                    |    "methodNamespace": "${namespace1.get}",
                                    |    "methodName": "${name1.get}",
                                    |    "methodVersion": 1
                                    |  },
                                    |  "outputs": {
                                    |    "CancerExomePipeline_v2.M2.m2_output_vcf": "this.OUTPUT_VCF"
                                    |  },
                                    |  "inputs": {
                                    |    "CancerExomePipeline_v2.M2.intervals": "this.ref_intervals",
                                    |    "CancerExomePipeline_v2.M2.tumor_bam": "this.case_bam",
                                    |    "CancerExomePipeline_v2.M2.normal_bam": "this.control_bam",
                                    |    "CancerExomePipeline_v2.M2.ref_fasta_dict": "this.ref_dict",
                                    |    "CancerExomePipeline_v2.M2.tumor_bai": "this.case_bai",
                                    |    "CancerExomePipeline_v2.M2.normal_bai": "this.control_bai",
                                    |    "CancerExomePipeline_v2.M2.ref_fasta": "this.ref_fasta",
                                    |    "CancerExomePipeline_v2.M2.ref_fasta_fai": "this.ref_fai",
                                    |    "CancerExomePipeline_v2.M2.m2_output_vcf_name": "this.vcf_output_name"
                                    |  },
                                    |  "rootEntityType": "pair",
                                    |  "prerequisites": {},
                                    |  "deleted": false,
                                    |  "namespace": "${namespace1.get}"
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
    entityType = Option(AgoraEntityType.Workflow),
    snapshotComment = snapshotComment1
  )

  val testEntity7 = AgoraEntity(namespace = namespace1,
    name = name2,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload2,
    entityType = Option(AgoraEntityType.Task),
    snapshotComment = snapshotComment1
  )

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

  val testMethodWithSnapshot1 = new AgoraEntity(
    namespace = namespace3,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow),
    snapshotId = snapshotId1
  )

  val testConfigWithSnapshot1 = new AgoraEntity(
    namespace = namespace3,
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = methodConfigPayload,
    entityType = Option(AgoraEntityType.Configuration),
    snapshotId = snapshotId1
  )

  val testConfigWithSnapshotMissingConfigVersion =
    testConfigWithSnapshot1.copy(name = name5, payload = methodConfigPayloadMissingConfigVersion)

  val testConfigWithSnapshotComment1 = testConfigWithSnapshot1.copy(snapshotComment = snapshotComment1, name = name4)

  val testMethodWithSnapshotComment1 = testMethodWithSnapshot1.copy(snapshotComment = snapshotComment1, name = name4)

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

  val testBadAgoraEntityWdlImportNotFound = testBadAgoraEntityNonExistentWdlImportFormat.copy(payload = payloadReferencingExternalMethodNotFound)

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

  val testIntegrationEntity = AgoraEntity(namespace = Option("___test1"),
    name = Option("testWorkflow"),
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner1,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testIntegrationEntity2 = AgoraEntity(namespace = Option("___test2"),
    name = Option("testWorkflow"),
    synopsis = synopsis1,
    documentation = documentation1,
    owner = owner2,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testIntegrationEntity3 = AgoraEntity(namespace = Option("___test3"),
    name = Option("testWorkflow3"),
    synopsis = synopsis3,
    documentation = documentation1,
    owner = owner3,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow))

  val testAgoraEntityWithValidOfficialDockerImageInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = payloadWithValidOfficialDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidOfficialDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = payloadWithInvalidOfficialDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidOfficialDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = payloadWithInvalidOfficialDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithValidPersonalDockerInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = payloadWithValidPersonalDockerImageInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerUserNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = payloadWithInvalidPersonalDockerUserNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerRepoNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = payloadWithInvalidPersonalDockerRepoNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithInvalidPersonalDockerTagNameInWdl = new AgoraEntity(namespace = Option("___docker_test"),
    name = name1,
    synopsis = synopsis1,
    documentation = documentation1,
    owner = mockAuthenticatedOwner,
    payload = payloadWithInvalidPersonalDockerTagNameInWdl,
    entityType = Option(AgoraEntityType.Task)
  )

  val testAgoraEntityWithAllLegalNameChars = new AgoraEntity(
    namespace = nameWithAllLegalChars,
    name = nameWithAllLegalChars,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow),
    snapshotId = snapshotId1
  )

  val testAgoraEntityWithIllegalNameChars = new AgoraEntity(
    namespace = nameWithAllLegalChars,
    name = badNameWithIllegalChars,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow),
    snapshotId = snapshotId1
  )

  val testAgoraEntityWithIllegalNamespaceChars = new AgoraEntity(
    namespace = badNameWithIllegalChars,
    name = nameWithAllLegalChars,
    payload = payload1,
    entityType = Option(AgoraEntityType.Workflow),
    snapshotId = snapshotId1
  )

}
