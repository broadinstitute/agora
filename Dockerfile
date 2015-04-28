# Production build/deploy/start of DSDE Methods Repository Webservice

# Basis image- Lukas K. recommended
FROM debian:jessie
MAINTAINER DSDE <dsde-engineering@broadinstitute.org>

# Set java environment variables
ENV JAVA_VERSION 7u75
ENV JAVA_DEBIAN_VERSION 7u75-2.5.4-2

# Install necessary packages including java 7 jre and sbt
RUN echo "deb http://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list
RUN apt-get update && apt-get install -y --force-yes \
        curl \
        git \
        openjdk-7-jre-headless="$JAVA_DEBIAN_VERSION" \
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
COPY build.sbt /usr/methodsrepo/build.sbt
COPY assembly.sbt /usr/methodsrepo/assembly.sbt
COPY project /usr/methodsrepo/project
COPY src /usr/methodsrepo/src

# Set the container's working directory
WORKDIR /usr/methodsrepo

# Build the web service application
RUN sbt assembly

# Start the webservice with default parameters
ENTRYPOINT ["java", "-jar", "target/scala-2.11/methods-0.1-SNAPSHOT.jar"]
