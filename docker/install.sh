#!/bin/bash

set -e

AGORA_DIR=$1
cd $AGORA_DIR
sbt assembly
AGORA_JAR=$(find target | grep 'agora.*\.jar')
mv $AGORA_JAR ./agora.jar
sbt clean
