[![Build Status](https://img.shields.io/travis/com/broadinstitute/agora)](https://travis-ci.com/broadinstitute/agora?branch=master)
[![Coverage Status](https://img.shields.io/codecov/c/gh/broadinstitute/agora)](https://codecov.io/gh/broadinstitute/agora)


Agora
=====

It's a methods repository!

Agora is written in [Scala](http://www.scala-lang.org/), uses [Akka](http://akka.io/)/[Akka-HTTP](http://akka.io/) for
its web framework, and is built using [SBT](www.scala-sbt.org/).

## Building and testing the Agora Web Service

To obtain and build the app, perform the following commands:

```
$ brew install git-secrets # if not already installed
$ git clone https://github.com/broadinstitute/agora.git
$ cd agora
$ sbt assembly
```

You can additionally run unit test through:

```
$ sbt test
```

### Building with docker

To build the `broadinstitute/agora` docker image, run
```
$ ./docker/build.sh jar -d build
```

This builds the agora jar, and copies it into the docker image (see `Dockerfile`).

## Running Your Own Agora Web Service

Assuming you have already built as per the instructions above, create a file called "/etc/agora.conf", with the appropriate configuration information populated.
An example, with defaults, can found in ```src/main/resources/reference.conf```.
Move application.conf to ```src/main/resources/```
Start a mongo database.
Finally, run the following command to start the server:

```
$ sbt run
```

Go to the specified web address in your favorite browser to test that it is operational.

## Running Your Own Agora Web Service Using Docker

Generate Agora configurations using the configurations in FireCloud-Develop:

```
APP_NAME=agora \
    ENV=dev \
    RUN_CONTEXT=local \
    INPUT_DIR=$PWD/../firecloud-develop \
    OUTPUT_DIR=$PWD/config \
    ../firecloud-develop/configure.rb
```

Launch the generated docker compose file:

```
docker-compose -p agora -f config/docker-compose.yaml up
```

or, use the config script:

```
./config/docker-rsync-local-agora.sh 
```

The docker compose configuration is set to point to https://local.broadinstitute.org:30443/ (where the endpoints can be viewed via Swagger).
