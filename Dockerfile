# Production build/deploy/start of DSDE Methods Repository Webservice

# TODO: adopt `sbtscala/scala-sbt:eclipse-temurin-jammy-11.0.22_7_1.9.9_2.13.13`
# Currently using the very outdated http://github.com/broadinstitute/scala-baseimage
# because the Docker version on Jenkins is obsolete and does not support recent images.
FROM broadinstitute/scala-baseimage as builder

# Install Agora
ADD . /agora
RUN ["/bin/bash", "-c", "/agora/docker/install.sh /agora"]

FROM us.gcr.io/broad-dsp-gcr-public/base/jre:11-debian

# Expose the port used by Agora webservice
EXPOSE 8000

COPY --from=builder /agora/agora.jar /agora/
COPY --from=builder /agora/docker/run.sh /agora/docker/
COPY --from=builder /agora/src/main/resources/db/migration/ /agora/src/migration

ENTRYPOINT ["/agora/docker/run.sh"]
