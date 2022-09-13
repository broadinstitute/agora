task grep {
  String pattern
  String? flags
  File file_name

  command {
    grep ${pattern} ${flags} ${file_name}
  }
  output {
    File out = "stdout"
  }
  runtime {
    memory: "2 MB"
    cpu: 1
    defaultDisks: "mydisk 3 LOCAL_SSD"
  }
}

task wc {
  Array[File]+ files

  command {
    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
  }
  output {
    Int count = read_int("stdout")
  }
}

workflow scatter_gather_grep_wc {
  Array[File] input_files

  scatter(f in input_files) {
    call grep {
      input: file_name = f
    }
  }
  call wc {
    input: files = grep.out
  }
}


