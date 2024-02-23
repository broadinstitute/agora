[![Build Status](https://img.shields.io/github/actions/workflow/status/broadinstitute/agora/unit_tests.yml)](https://github.com/broadinstitute/agora/actions/workflows/unit_tests.yml)
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

### Requirements:

* [Docker Desktop](https://www.docker.com/products/docker-desktop) (4GB+, 8GB recommended)
* Broad internal internet connection (or VPN, non-split recommended)
* Render the local configuration files. From the root of the Agora repo, run:
```sh
./local-dev/bin/render
```
### Running:

After satisfying the above requirements, execute the following command from the root of the Agora repo:

```sh
./config/docker-rsync-local-agora.sh
```

By default, this will set up an instance of rawls pointing to the database and Sam in dev. 
It will also set up a process that will watch the local files for changes, and restart the service when source files change.

See docker-rsync-local-rawls.sh for more configuration options.

The docker compose configuration is set to point to https://local.broadinstitute.org:30443/ (where the endpoints can be viewed via Swagger).
