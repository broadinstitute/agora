#!/bin/bash

function fail() {
    echo "FATAL: $@"
    exit 1
}

# Require a DEFS_FILE; if it doesn't exist, look for a file alongside
if [ -z $DEFS_FILE ]; then
    fail "No DEFS_FILE environment variable defined."
fi
if [ ! -f $DEFS_FILE ]; then
    fail "DEFS_FILE file path, $DEFS_FILE, does not point to a file."
fi
source $DEFS_FILE

# Run an agora docker container on port 8000
echo "Running agora-app docker container"
docker run -d \
	--name agora-app \
	-p 8000:8000 \
	broadinstitute/agora

# run an apache-auth-proxy container in front of the app
# note: server.crt and server.key must exist in the dir you run this container from
echo "Running agora-proxy apache auth/SSL proxy container"
docker run -d \
	-e AGENT_URL=$AGENT_URL \
	-e AGENT_PROFILE_NAME=$AGENT_PROFILE_NAME \
	-e AM_SERVER_URL=https:$AM_SERVER_URL \
	-e PASSWORD=$PASSWORD \
	-e REALM=$REALM \
	-e SERVER_ADMIN=$SERVER_ADMIN \
	-e SERVER_NAME=$SERVER_NAME \
	-e PROXY_URL=$PROXY_URL \
	-p 80:80 \
	-p 443:443 \
	-v `pwd`/agora.broadinstitute.org.crt:/usr/local/apache2/conf/server.crt \
	-v `pwd`/agora.broadinstitute.org.key:/usr/local/apache2/conf/server.key \
	-v `pwd`/incommon256.crt:/usr/local/apache2/conf/ca-bundle.crt \
	--link agora-app:agora-app \
	--name agora-proxy \
	broadinstitute/apache-auth-proxy
