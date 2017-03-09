# Production build/deploy/start of DSDE Methods Repository Webservice

# http://github.com/broadinstitute/scala-baseimage
FROM broadinstitute/scala-baseimage

# Expose the port used by Agora webservice
EXPOSE 8000

#jprofiler
RUN wget http://download-aws.ej-technologies.com/jprofiler/jprofiler_linux_9_2.tar.gz && tar -xvf jprofiler_linux_9_2.tar.gz && rm jprofiler_linux_9_2.tar.gz

# Install Agora
ADD . /agora
RUN ["/bin/bash", "-c", "/agora/docker/install.sh /agora"]

# Add Agora as a service (it will start when the container starts)
RUN mkdir /etc/service/agora && \
    mkdir /var/log/agora && \
    cp /agora/docker/run.sh /etc/service/agora/run
