[![Build Status](https://travis-ci.org/broadinstitute/agora.svg?branch=master)](https://travis-ci.org/broadinstitute/agora?branch=master)
[![Coverage Status](https://coveralls.io/repos/broadinstitute/agora/badge.svg?branch=master)](https://coveralls.io/r/broadinstitute/agora?branch=master)


Agora
=====

It's a methods repository!

Agora is written in [Scala](http://www.scala-lang.org/), uses [Akka](http://akka.io/)/[Spray](http://spray.io/) for its web framework, and is built using [SBT](www.scala-sbt.org/).

## Building and testing the Agora Web Service

To obtain and build the app, perform the following commands:

```
$ git clone https://github.com/broadinstitute/agora.git
$ cd agora
$ sbt package
```

You can additionally run unit test through:

```
$ sbt unitTest
```

and integration test using:

```
$ sbt integrationTest
```

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
    ENV=local \
    OUTPUT_DIR=./config \
    ../firecloud-develop/configure.rb
```

Launch the generated docker compose file:

```
docker-compose -p agora -f config/docker-compose.yaml up
```
