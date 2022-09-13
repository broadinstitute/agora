#!/bin/bash
set -euox pipefail
IFS=$'\n\t'

cd /agora
sbt ~reStart
