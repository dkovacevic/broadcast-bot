FROM dejankovacevic/bots.runtime:2.10.2

COPY target/channel.jar   /opt/channel/channel.jar
COPY channel.yaml         /etc/channel/channel.yaml

WORKDIR /opt/channel
EXPOSE  8080 8081 8082
