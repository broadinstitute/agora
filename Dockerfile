# Production build/deploy/start of DSDE Methods Repository Webservice

# Basis image- Lukas K. recommended
FROM debian:jessie
MAINTAINER DSDE <dsde-engineering@broadinstitute.org>


# Install necessary packages including java 8 jre and sbt and clean up apt caches
RUN echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" >> /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
    echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    echo debconf shared/accepted-oracle-license-v1-1 seen true | /usr/bin/debconf-set-selections

RUN apt-get update && \
    apt-get --no-install-recommends install -y --force-yes \
        oracle-java8-installer \
    apt-get clean autoclean && \
    apt-get autoremove -y && \
    rm -rf /var/lib/{apt,dpkg,cache,log}/ /var/cache/oracle-jdk8-installer 

# Expose the port used by the webservice
EXPOSE 8000

WORKDIR /usr/agora

COPY agora-0.1-SNAPSHOT.jar /usr/agora/

# Start the webservice with default parameters do not use default config
ENTRYPOINT ["java", "-Dconfig.file=/etc/agora.conf" ,"-jar", "agora-0.1-SNAPSHOT.jar"]
