# Production build/deploy/start of DSDE Methods Repository Webservice

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

ENTRYPOINT ["/agora/docker/run.sh"]
