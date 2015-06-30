# Production build/deploy/start of DSDE Methods Repository Webservice

# Basis image- Lukas K. recommended
FROM debian:jessie
MAINTAINER DSDE <dsde-engineering@broadinstitute.org>

# Install necessary packages including java 8 jre and sbt and clean up apt caches
RUN echo "deb http://dl.bintray.com/sbt/debian /" >> /etc/apt/sources.list.d/sbt.list && \
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | /usr/bin/debconf-set-selections

RUN apt-get update && \
    apt-get --no-install-recommends install -y --force-yes \
        oracle-java8-installer \
        sbt && \
    apt-get clean autoclean && \
    apt-get autoremove -y && \
    rm -rf /var/lib/{apt,dpkg,cache,log}/ /var/cache/oracle-jdk8-installer 

# Expose the port used by the webservice
EXPOSE 8000

# Assumes Dockerfile lives in root of the git repo. Pull source files into container
COPY build.sbt /usr/agora/build.sbt
COPY assembly.sbt /usr/agora/assembly.sbt
COPY project /usr/agora/project

WORKDIR /usr/agora

# Run update before we pull down the source so we can cache dependencies and not re-download every time the src changes
RUN sbt update

COPY src /usr/agora/src

# Run the tests then copy in the application.conf file and build the jar. Then copy the jar in to the work directory and
# do some clean up in order to minimize image size.
RUN sbt test && \
    cp application.conf src/main/resources/application.conf && \
    sbt assembly && \
    cp /usr/agora/target/scala-2.11/agora-0.1-SNAPSHOT.jar /usr/agora && \
    rm -rf /root/.embedmongo /root/.ivy2 /root/.sbt /usr/agora/target /usr/agora/src /usr/agora/build.sbt /usr/agora/assembly.sbt

# Start the webservice with default parameters do not use default config
ENTRYPOINT ["java", "-Dconfig.file=/etc/agora.conf" ,"-jar", "agora-0.1-SNAPSHOT.jar"]
