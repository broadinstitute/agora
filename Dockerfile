# Production build/deploy/start of DSDE Methods Repository Webservice

# http://github.com/broadinstitute/scala-baseimage
FROM broadinstitute/scala-baseimage as builder

# Install Agora
ADD . /agora
RUN ["/bin/bash", "-c", "/agora/docker/install.sh /agora"]

FROM adoptopenjdk:11-hotspot

# Expose the port used by Agora webservice
EXPOSE 8000

COPY --from=builder /agora/agora.jar /agora/
COPY --from=builder /agora/docker/run.sh /agora/docker/
COPY --from=builder /agora/src/main/resources/db/migration/ /agora/src/migration

# 1. "Exec" form of CMD necessary to avoid `sh` stripping environment variables with periods in them,
#    used for Lightbend config
# 2. $JAVA_OPTS required for configuration
# We use the "exec" form but call `bash` to accomplish both 1 and 2
CMD ["/bin/bash", "-c", "java $JAVA_OPTS -Dconfig.file=/etc/agora.conf -jar /agora/agora.jar"]
