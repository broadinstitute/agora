task wc {
  Array[File]+ files

  command {
    wc -l ${sep=' ' files} | tail -1 | cut -d' ' -f 2
  }
  output {
    Int count = read_int("stdout")
  }
}

workflow foo {}