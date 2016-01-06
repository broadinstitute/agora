#!/bin/bash

set -e
java $JAVA_OPTS -Djava.library.path=./native -Dconfig.file=/etc/agora.conf -javaagent:/agora/agora.jar -jar /agora/agora.jar
