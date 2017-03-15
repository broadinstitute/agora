#!/bin/bash

set -e
java $JAVA_OPTS -Dconfig.file=/etc/agora.conf -agentpath:/jprofiler9/bin/linux-x64/libjprofilerti.so=port=8849,nowait -jar /agora/agora.jar

