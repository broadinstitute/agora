[![Build Status](https://travis-ci.org/broadinstitute/agora.svg?branch=master)](https://travis-ci.org/broadinstitute/agora?branch=master)
[![Coverage Status](https://coveralls.io/repos/broadinstitute/agora/badge.svg?branch=master)](https://coveralls.io/r/broadinstitute/agora?branch=master)


Agora
=====

It's a methods repository!

Agora is written in [Scala](http://www.scala-lang.org/), uses [Akka](http://akka.io/)/[Spray](http://spray.io/) for its web framework, and is built using [SBT](www.scala-sbt.org/).

## Building and testing the Agora Web Service

To obtain and build the app, perform the following commands:

```
$ brew install git-secrets # if not already installed
$ git clone https://github.com/broadinstitute/agora.git
$ cp -r hooks/ .git/hooks/ # this step can be skipped if you use the rsync script to spin up locally
$ chmod 755 .git/hooks/apply-git-secrets.sh # this step as well
$ cd agora
$ sbt package
```

You can additionally run unit test through:

```
$ sbt test
```

Make sure your config file isn't using the actual dev databases...

and integration test using:

```
$ sbt integrationTest
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
    INPUT_DIR=$PWD \
    OUTPUT_DIR=./config \
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
