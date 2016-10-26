#!/bin/bash

set -e
java $JAVA_OPTS -Djava.library.path=./native -Dconfig.file=/etc/agora.conf -jar /agora/agora.jar
