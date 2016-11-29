#!/bin/bash

set -e
java $JAVA_OPTS -Dconfig.file=/etc/agora.conf -jar /agora/agora.jar
