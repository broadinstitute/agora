#!/bin/bash

set -e

function make_jar() {
    echo "building jar..."
    docker run --rm -v $PWD:/working \
            -v jar-cache:/root/.ivy \
            -v jar-cache:/root/.ivy2 \
            broadinstitute/scala-baseimage /working/docker/install.sh /working
}

function docker_cmd()
{
    if [ $DOCKER_CMD = "build" ] || [ $DOCKER_CMD = "push" ]; then
        echo "building docker image..."
        GIT_SHA=$(git rev-parse ${GIT_BRANCH})
        echo GIT_SHA=$GIT_SHA > env.properties  # for jenkins jobs
        docker build --no-cache -t $REPO:${GIT_SHA:0:12} .

        if [ $DOCKER_CMD = "push" ]; then
            echo "pushing docker image..."
            docker push $REPO:${GIT_SHA:0:12}
        fi
    else
        echo "Not a valid docker option!  Choose either build or push (which includes build)"
    fi
}

# parse command line options
DOCKER_CMD=
GIT_BRANCH=${GIT_BRANCH:-$(git rev-parse --abbrev-ref HEAD)}  # default to current branch
REPO=${REPO:-broadinstitute/$PROJECT}  # default to rawls docker repo
while [ "$1" != "" ]; do
    case $1 in
        jar) make_jar ;;
        -d | --docker) shift
                       echo $1
                       DOCKER_CMD=$1
                       docker_cmd
                       ;;
    esac
    shift
done
