#!/bin/bash

set -e

cd /agora
sbt "~reStart --- $JAVA_OPTS"
