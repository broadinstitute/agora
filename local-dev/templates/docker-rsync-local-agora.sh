#!/bin/bash
# v1.0 zarsky@broad
hash fswatch 2>/dev/null || {
    echo >&2 "This script requires fswatch (https://github.com/emcrisostomo/fswatch), but it's not installed. On Darwin, just \"brew install fswatch\".  Aborting."; exit 1;
}

MONGODB_K8S_DOMAIN=terra-dev.svc.cluster.local
MONGODB_0_ADDR=35.202.221.5
MONGODB_1_ADDR=130.211.207.88
MONGODB_2_ADDR=104.154.193.38

if [ -a ./.docker-rsync-local.pid ]; then
    echo "Looks like clean-up wasn't completed, doing it now..."
    docker rm -f agora-rsync-container agora-proxy agora-sbt
    docker network rm fc-agora
    pkill -P $(< "./.docker-rsync-local.pid")
    rm ./.docker-rsync-local.pid
fi

clean_up () {
    echo
    echo "Cleaning up after myself..."
    docker rm -f agora-rsync-container agora-proxy agora-sbt sqlproxy
    docker network rm fc-agora
    pkill -P $$
    rm ./.docker-rsync-local.pid
}
trap clean_up EXIT HUP INT QUIT PIPE TERM 0 20

#ensure git secrets hooks in place
cp -r ./hooks/ ./.git/hooks/
chmod 755 ./.git/hooks/apply-git-secrets.sh

echo "Launching rsync container..."
docker run -d \
    --name agora-rsync-container \
    -v agora-shared-source:/working \
    -e DAEMON=docker \
    tjamet/rsync

run_rsync ()  {
    rsync --blocking-io -azl --delete -e "docker exec -i" . agora-rsync-container:working \
        --filter='+ /build.sbt' \
        --filter='+ /config/***' \
        --filter='- /project/project/target/***' \
        --filter='- /project/target/***' \
        --filter='+ /project/***' \
        --filter='+ /src/***' \
        --filter='+ /.git/***' \
        --filter='- *'
}
echo "Performing initial file sync..."
run_rsync
fswatch -o . | while read f; do run_rsync; done &
echo $$ > ./.docker-rsync-local.pid

start_server () {
    docker network create fc-agora

    echo "Creating Google sqlproxy container..."
    docker create --name sqlproxy \
        --restart "always" \
        --network="fc-agora" \
        --env-file="./config/sqlproxy.env" \
        broadinstitute/cloudsqlproxy:1.11_20180808

    docker cp config/sqlproxy-service-account.json sqlproxy:/etc/sqlproxy-service-account.json

    echo "Creating SBT docker container..."
    docker create -it --name agora-sbt \
    -v agora-shared-source:/app -w /app \
    -v jar-cache:/root/.ivy -v jar-cache:/root/.ivy2 \
    -p 35051:5051 \
    --add-host "mongodb-0.mongodb-headless:${MONGODB_0_ADDR}" \
    --add-host "mongodb-0.mongodb-headless.${MONGODB_K8S_DOMAIN}:${MONGODB_0_ADDR}" \
    --add-host "mongodb-1.mongodb-headless:${MONGODB_1_ADDR}" \
    --add-host "mongodb-1.mongodb-headless.${MONGODB_K8S_DOMAIN}:${MONGODB_1_ADDR}" \
    --add-host "mongodb-2.mongodb-headless:${MONGODB_2_ADDR}" \
    --add-host "mongodb-2.mongodb-headless.${MONGODB_K8S_DOMAIN}:${MONGODB_2_ADDR}" \
    --network=fc-agora \
    -e JAVA_OPTS='-Dconfig.file=/app/config/agora.conf' \
    hseeberger/scala-sbt:eclipse-temurin-17.0.2_1.6.2_2.13.8 \
    sbt \~reStart

    docker cp config/agora-account.pem agora-sbt:/etc/agora-account.pem

    echo "Creating proxy..."
    docker create --name agora-proxy \
    --restart "always" \
    --network=fc-agora \
    -p 30080:80 -p 30443:443 \
    -e PROXY_URL='http://agora-sbt:8000/' \
    -e PROXY_URL2='http://agora-sbt:8000/api' \
    -e PROXY_URL3='http://agora-sbt:8000/register' \
    -e CALLBACK_URI='https://local.broadinstitute.org/oauth2callback' \
    -e LOG_LEVEL='debug' \
    -e SERVER_NAME='local.broadinstitute.org' \
    -e APACHE_HTTPD_TIMEOUT='650' \
    -e APACHE_HTTPD_KEEPALIVE='On' \
    -e APACHE_HTTPD_KEEPALIVETIMEOUT='650' \
    -e APACHE_HTTPD_MAXKEEPALIVEREQUESTS='500' \
    -e APACHE_HTTPD_PROXYTIMEOUT='650' \
    -e PROXY_TIMEOUT='650' \
    -e REMOTE_USER_CLAIM='sub' \
    -e ENABLE_STACKDRIVER='yes' \
    -e FILTER2='AddOutputFilterByType DEFLATE application/json text/plain text/html application/javascript application/x-javascript' \
    us.gcr.io/broad-dsp-gcr-public/httpd-terra-proxy:v0.1.16
    
    docker cp config/server.crt agora-proxy:/etc/ssl/certs/server.crt
    docker cp config/server.key agora-proxy:/etc/ssl/private/server.key
    docker cp config/ca-bundle.crt agora-proxy:/etc/ssl/certs/server-ca-bundle.crt
    docker cp config/oauth2.conf agora-proxy:/etc/httpd/conf.d/oauth2.conf
    docker cp config/site.conf agora-proxy:/etc/httpd/conf.d/site.conf

    echo "Running at https://local.broadinstitute.org:30443"
    
    echo "Starting sqlproxy..."
    docker start sqlproxy
    echo "Starting proxy..."
    docker start agora-proxy
    echo "Starting SBT..."
    docker start -ai agora-sbt
}
start_server
