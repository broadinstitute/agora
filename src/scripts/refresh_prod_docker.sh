#!/bin/sh

echo "Stopping and removing the app container"
docker stop agora-app
sudo docker rm agora-app

echo "Stopping and removing the proxy container"
docker stop agora-proxy
docker rm agora-proxy

echo "Pulling the latest version of the agora api docker image"
docker pull broadinstitute/agora

echo "Executing the run script"
./run_prod_docker.sh
