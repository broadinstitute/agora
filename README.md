[![Build Status](https://travis-ci.org/broadinstitute/agora.svg?branch=master)](https://travis-ci.org/broadinstitute/agora?branch=master)
[![Coverage Status](https://coveralls.io/repos/broadinstitute/agora/badge.svg?branch=master)](https://coveralls.io/r/broadinstitute/agora?branch=master)


Agora
=====

It's a methods repository!

Agora is written in [Scala](http://www.scala-lang.org/), uses [Akka](http://akka.io/)/[Spray](http://spray.io/) for its web framework, and is built using [SBT](www.scala-sbt.org/).

## Testing and Running the Agora Web Service

To obtain and build the app, perform the following commands:

```
$ git clone https://github.com/broadinstitute/agora.git
$ cd agora
```

## Unit Tests

```
$ sbt clean test
```

The unit tests run in forked mode against a test configuration to 
prevent accidentally hitting real databases.  

## Running Your Own Agora Web Service Using Docker

Generate Agora configurations using the configurations defined 
in FireCloud-Develop:

```
APP_NAME=agora \ 
    ENV=local \
    OUTPUT_DIR=./config \
    ../firecloud-develop/configure.rb
```

Launch the generated docker compose file:

```
docker-compose -p agora -f config/docker-compose.yaml up
```
The docker compose configuration is set to point to 
[https://local.broadinstitute.org:30443](https://local.broadinstitute.org:30443)