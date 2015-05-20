# Production build/deploy/start of DSDE Methods Repository Webservice

# Basis image- Lukas K. recommended
FROM debian:jessie
MAINTAINER DSDE <dsde-engineering@broadinstitute.org>

# Install necessary packages including java 8 jre and sbt
RUN echo "deb http://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list && \
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> -a /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | /usr/bin/debconf-set-selections

RUN apt-get update && apt-get install -y --force-yes \
        curl \
        git \
        oracle-java8-installer \
        sbt \
        sudo \
        ssh \
        unzip \
        vim \
        wget \
        zip

# Expose the port used by the webservice
EXPOSE 8000

# Assumes Dockerfile lives in root of the git repo. Pull source files into container
COPY build.sbt /usr/agora/build.sbt
COPY assembly.sbt /usr/agora/assembly.sbt
COPY project /usr/agora/project
COPY src /usr/agora/src

# Set the container's working directory
WORKDIR /usr/agora

# Build the web service application
RUN sbt assembly

# Start the webservice with default parameters
# This is an interim solution to get around us not using application.conf. Without this change we would not be able to use vault common for openam configuration.
ENTRYPOINT ["java", "-Dconfig.resource=agora.conf", "-cp", "/etc:target/scala-2.11/agora-0.1-SNAPSHOT.jar", "org.broadinstitute.dsde.agora.server.Agora"]
