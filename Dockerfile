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

# needed to re-trigger Notary check, will revert
ENV version=0.0.1

ENTRYPOINT ["/agora/docker/run.sh"]
